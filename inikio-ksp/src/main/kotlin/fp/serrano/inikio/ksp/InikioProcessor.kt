package fp.serrano.inikio.ksp

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.*
import fp.serrano.inikio.plugin.*
import fp.serrano.inikio.ProgramBuilder
import kotlin.coroutines.RestrictsSuspension

@OptIn(KspExperimental::class)
class InikioProcessor(
  private val environment: SymbolProcessorEnvironment
): SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getNewFiles().forEach { file ->
      file
        .declarations
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.isAnnotationPresent(InitialStyleDSL::class) }
        .forEach { it.process(file) }
    }

    return emptyList()
  }

  private fun KSClassDeclaration.process(originalFile: KSFile) {
    val klass = this
    val klassName = simpleName.asString()
    val hasTypeArgs = typeParameters.isNotEmpty()
    val typeArgs = typeParameters.map { TypeVariableName(it.name.asString()) }
    val builderName = "${klassName}Builder"
    val builderFullName = ClassName(packageName.asString(), builderName)

    val doneClass = this.findDoneClass()
    if (doneClass == null) {
      environment.logger.error("No 'done class' found")
      return
    }

    BuilderWrapper(FileSpec.builder(packageName.asString(), builderName), { build() }) {

      val originalType =
        if (hasTypeArgs) klass.toClassName().parameterizedBy(typeArgs) else klass.toClassName()
      val programBuilderType =
        ProgramBuilder::class.asClassName().parameterizedBy(
          originalType,
          resultType()
        )

      `do` {
        addType(
          BuilderWrapper(TypeSpec.classBuilder(builderName), { build() }) {
            `do` { addAnnotation(RestrictsSuspension::class) }
            if (hasTypeArgs) {
              `do` { addTypeVariables(typeArgs) }
            }
            `do` { superclass(programBuilderType) }
            `do` {
              val doneClassQName = doneClass.qualifiedName?.asString() ?: doneClass.simpleName.asString()
              addSuperclassConstructorParameter(
                "{ result -> $doneClassQName(result) }"
              )
            }

            klass.instructionClasses().forEach { instr ->
              val instrName = instr.simpleName.asString()
              val instrQName = instr.qualifiedName?.asString() ?: instrName
              val instrProperties = instr.getDeclaredProperties().toList()
              val instrArgs = instrProperties.dropLast(1)
              val continuation = instrProperties.last()

              val continuationType = when (val t = continuation.type.toTypeName(typeParameters.toTypeParameterResolver())) {
                is ParameterizedTypeName ->
                  if (t.rawType.canonicalName == "kotlin.Function0") UNIT
                  else t.typeArguments.firstOrNull()
                is LambdaTypeName -> t.parameters.firstOrNull()?.type
                else -> null
              } ?: UNIT

              `do` {
                addFunction(
                  BuilderWrapper(FunSpec.builder(instrName.firstLower()), { build() }) {
                    `do` { addModifiers(KModifier.SUSPEND) }
                    instrArgs.forEach { property ->
                      `do` {
                        addParameter(
                          ParameterSpec.builder(
                            property.simpleName.asString(),
                            property.type.toTypeName(typeParameters.toTypeParameterResolver())
                          ).build()
                        )
                      }
                    }
                    `do` { returns(continuationType) }
                    val args = instrArgs.map { it.simpleName.asString() } + listOf("arg")
                    `do` {
                      when (continuationType) {
                        UNIT -> addStatement(
                          "return performUnit { arg -> ${instrQName}(${args.joinToString(separator = ", ")}) }"
                        )
                        else -> addStatement(
                          "return perform { arg -> ${instrQName}(${args.joinToString(separator = ", ")}) }"
                        )
                      }
                    }
                  }
                )
              }
            }
          }
        )
      }

      `do` {
        addFunction(
          BuilderWrapper(FunSpec.builder(klassName.firstLower()), { build() }) {
            if (hasTypeArgs) {
              `do` { addTypeVariables(typeArgs) }
            }
            `do` {
              addParameter(
                ParameterSpec.builder(
                  "block",
                  LambdaTypeName.get(
                    receiver =
                      if (hasTypeArgs) builderFullName.parameterizedBy(typeArgs) else builderFullName,
                    returnType = resultType()
                  ).copy(suspending = true)
                ).build()
              )
            }
            `do` { returns(originalType) }
            `do` {
              addStatement(
                "return fp.serrano.inikio.program(${builderName}(), block)"
              )
            }
          }
        )
      }

    }.writeTo(environment.codeGenerator, true, listOf(originalFile))
  }

  private fun KSClassDeclaration.resultType(): TypeName {
    val resultAnnotations = getAnnotationsByType(FixedResultType::class).toList()
    return when {
      resultAnnotations.isEmpty() -> typeParameters.last().toTypeVariableName()
      else -> ClassName.bestGuess(resultAnnotations.first().type)
    }
  }

  private fun KSClassDeclaration.findDoneClass(): KSClassDeclaration? =
    getSealedSubclasses().find { subclass ->
      val properties = subclass.getDeclaredProperties()
      properties.count() == 1 &&
              properties.all { property ->
                !property.type.resolve().isFunctionType
              }
    }

  private fun KSClassDeclaration.instructionClasses(): Sequence<KSClassDeclaration> =
    getSealedSubclasses().filter { subclass ->
      subclass.getDeclaredProperties().last().type.resolve().isFunctionType
    }
}

fun String.firstLower() = replaceFirstChar { if (it.isUpperCase()) it.lowercaseChar() else it }