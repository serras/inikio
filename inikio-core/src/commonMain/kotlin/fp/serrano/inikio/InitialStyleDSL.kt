package fp.serrano.inikio

/**
 * Instructs inikio's KSP plug-in to create a Builder based on this hierarchy.
 */
public annotation class InitialStyleDSL()

/**
 * Indicates that this DSL has a fixed result type, instead of being polymorphic
 * on the result type.
 *
 * The [type] must be a fully-qualified name. Unfortunately we cannot use a Class
 * here because of limitations of KSP + Kotlin Multiplatform.
 */
public annotation class FixedResultType(val type: String)

