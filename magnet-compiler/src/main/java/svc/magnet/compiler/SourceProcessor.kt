package svc.magnet.compiler

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import svc.magnet.annotation.MagnetNode
import svc.magnet.annotation.Source
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import svc.magnet.annotation.Block
import javax.tools.Diagnostic

@AutoService(Processor::class)
//@SupportedOptions("moduleName")// 可通过kapt的arg{key:value}传递参数，moduleName即为key
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("svc.magnet.annotation.Source")
class SourceProcessor : AbstractProcessor() {
    companion object {
        private const val LOG_TAG = "Magnet Compiler"
        private const val SOURCE_OPTION = "kapt.kotlin.generated"
    }

    private lateinit var mTypes: Types
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private val gson by lazy { Gson() }
    private lateinit var elementUtils: Elements

    private val sourceList by lazy { mutableListOf<String>() }

    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        filer = env.filer
        mTypes = env.typeUtils
        elementUtils = env.elementUtils
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val sourceClassList = roundEnv.getElementsAnnotatedWith(Source::class.java)
        if (sourceClassList.size <= 0) {
            return true
        }

        // 统计source
        for (element in sourceClassList) {
            sourceList.add(element.asType().asTypeName().toString())
        }

        for (element in sourceClassList) {
            val sourceClassName = getSourceName(element)
            val sourceType = MagnetNode::class.asTypeName().parameterizedBy(TypeVariableName(sourceClassName))

            val typeSpec = TypeSpec.classBuilder(sourceClassName)
                    .addSuperinterface(sourceType)
                    .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
                    .addType(buildFieldCompanion(element))
                    .addFunction(buildReplaceFun(element, sourceClassName))
                    .addFunction(buildObserveFun(element))
                    .addModifiers(KModifier.PUBLIC)

            val fileSpec = FileSpec.builder(getPackageName(element), sourceClassName)

            element.enclosedElements.forEach {
                if (it.kind == ElementKind.FIELD) {
                    typeSpec.addProperty(buildPropertyOrigin(it))
                    typeSpec.addProperty(buildPropertyMagnet(it))
                    fileSpec.addTypeAlias(buildPropertyTypeAlias(element, it))
                }
            }

            fileSpec.addType(typeSpec.build()).build().writeFile()
        }

        return true
    }

    fun log(log: String) {
        messager.printMessage(Diagnostic.Kind.WARNING, "$LOG_TAG $log")
    }

    fun toJson(what: Any): String {
        return gson.toJson(what)
    }

    fun FileSpec.writeFile() {

        val kaptKotlinGeneratedDir = processingEnv.options[SOURCE_OPTION]
        val outputFile = File(kaptKotlinGeneratedDir).apply {
            mkdirs()
        }
        writeTo(outputFile)
    }

    private fun buildReplaceFun(element: Element, selfName: String): FunSpec {
        val funSpec = FunSpec.builder("onReplace")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("source", ClassName.bestGuess(selfName))

        element.enclosedElements.forEach {
            if (it.kind == ElementKind.FIELD) {
                funSpec.addStatement("%L = source.%L", it.simpleName.toString(), it.simpleName.toString())
            }
        }

        return funSpec.build()
    }

    private fun buildObserveFun(element: Element): FunSpec {
        val blockType = Block::class.asTypeName().parameterizedBy(TypeVariableName("V"))
        val funSpec = FunSpec.builder("observe")
                .addTypeVariable(TypeVariableName.invoke("V : Any"))
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("key", String::class, KModifier.VARARG)
                .addParameter("block", blockType)

        val valueList = mutableListOf<Any>()
        valueList.add("key")
        val magnetClass = ClassName.bestGuess("com.svc.magnet.Magnet")
        val nodeMagnetClass = ClassName.bestGuess("com.svc.magnet.NodeMagnet")

        //==========================================================================================================
        funSpec.addStatement("when (%L[0]) {", "key")
        element.enclosedElements.forEach {
            if (it.kind == ElementKind.FIELD) {
                funSpec.addStatement("\t%L -> {", humpToUnderline(it))

                val isSource = isSource(it)
                val suitableMagnetClass = if (isSource) nodeMagnetClass else magnetClass
                val magnetField = getMagnetName(it)
                funSpec.addStatement("\t\tif (%L == null) %L = %T()", magnetField, magnetField, suitableMagnetClass)
                if (isSource) {
                    funSpec.addStatement("\t\tif (%L.size > 1) %L?.observe(*%L.toMutableList().subList(1, key.size).toTypedArray(), block = %L)", "key", magnetField, "key", "block")
                    funSpec.addStatement("\t\telse %L?.observe(%L)", magnetField, "block")
                } else {
                    funSpec.addStatement("\t\t%L?.observe { %L.onNext(it as V) }", magnetField, "block")
                }

                funSpec.addStatement("\t}\n")
            }
        }
        funSpec.addStatement("}")
        //==========================================================================================================

        return funSpec.build()
    }

    /**
     * 考虑到有些类原来有注解，例如ROOM数据库注解
     * 不方便把注解直接搬过来，magnet属性不需要存入数据库等
     * 做一个转换功能，这个功能可以放在 @Source 中做一个变量控制是否需要转换
     */
    private fun buildFromFun() {

    }

    /**
     * 考虑到有些类原来有注解，例如ROOM数据库注解
     * 不方便把注解直接搬过来，magnet属性不需要存入数据库等
     * 做一个转换功能，这个功能可以放在 @Source 中做一个变量控制是否需要转换
     */
    private fun buildBackFun() {

    }

    private fun buildPropertyOrigin(element: Element): PropertySpec {
        val name = element.simpleName.toString()
        val fieldType = element.asType().asTypeName().asNullable()
        val isSource = isSource(element)

        val propertySpec = if (isJavaTypeString(fieldType)) {
            PropertySpec.varBuilder(name, String::class.asTypeName().asNullable())
        } else {
            if (isSource) {
                PropertySpec.varBuilder(name, ClassName.bestGuess(element.asType().asTypeName().toString().plus("Source")).asNullable())
            } else {
                PropertySpec.varBuilder(name, fieldType)
            }
        }

        return propertySpec.addModifiers(KModifier.PUBLIC)
                .initializer("null")
                .setter(FunSpec.setterBuilder()
                        .addParameter("value", fieldType)
                        .addStatement("%L = %L", "field", "value")
                        .addStatement("%L?.value(%L)", getMagnetName(element), "value")
                        .build())
                .build()
    }

    private fun buildPropertyMagnet(element: Element): PropertySpec {
//        val magnetClass = ClassName.bestGuess("com.svc.magnet.Magnet")
        val isSource = isSource(element)
        val magnetClass = if (isSource) {
            ClassName.bestGuess("com.svc.magnet.NodeMagnet")
        } else {
            ClassName.bestGuess("com.svc.magnet.Magnet")
        }

        val fieldType = element.asType().asTypeName()
        val type = if (isJavaTypeString(fieldType)) {
            magnetClass.parameterizedBy(String::class.asTypeName()).asNullable()
        } else {
            if (isSource) {
                magnetClass.parameterizedBy(ClassName.bestGuess(fieldType.toString().plus("Source"))).asNullable()
            } else {
                magnetClass.parameterizedBy(fieldType).asNullable()
            }
        }

        return PropertySpec.varBuilder(getMagnetName(element), type)
                .initializer("null")
                .addModifiers(KModifier.PRIVATE).build()
    }

    private fun buildFieldCompanion(element: Element): TypeSpec {
        val typeSpec = TypeSpec.companionObjectBuilder()

        element.enclosedElements.forEach {
            if (it.kind == ElementKind.FIELD) {
                val fieldName = it.simpleName.toString()
                val keyName = humpToUnderline(it)
                typeSpec.addProperty(PropertySpec.builder(keyName, String::class, KModifier.PUBLIC)
                        .initializer("%S", fieldName)
                        .build())
            }
        }

        return typeSpec.build()
    }

    private fun buildPropertyTypeAlias(clazz: Element, property: Element): TypeAliasSpec {
        val aliasName = getSourceName(clazz).plus("_".plus(humpToUnderline(property))).toUpperCase()
        val fieldType = property.asType().asTypeName()
        val type = if (isJavaTypeString(fieldType)) {
            String::class.asTypeName()
        } else {
            if (isSource(property)) {
                ClassName.bestGuess(fieldType.toString().plus("Source"))
            } else {
                fieldType
            }
        }

        return TypeAliasSpec.builder(aliasName, type).build()
    }

    private fun isJavaTypeString(typeName: TypeName): Boolean {
        return typeName.toString().contains("java.lang.String")
    }

    private fun getMagnetName(element: Element): String {
        return element.simpleName.toString().plus("Magnet")
    }

    private fun getSourceName(element: Element): String {
        return element.simpleName.toString().plus("Source")
    }

    private fun isSource(element: Element): Boolean {
        return sourceList.contains(element.asType().asTypeName().toString())
    }

    private fun getPackageName(element: Element): String {
        return element.asType().asTypeName().toString().replace(".".plus(element.simpleName.toString()), "")
    }

    /**
     * 分离驼峰命名
     */
    private fun humpToUnderline(element: Element): String {
        val name = element.simpleName.toString()
        val sb = StringBuilder(name)
        var temp = 0//定位
        for (i in 0 until name.length) {
            if (Character.isUpperCase(name[i])) {
                sb.insert(i + temp, "_")
                temp += 1
            }
        }
        return sb.toString().toUpperCase()
    }
}