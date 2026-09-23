package com.qvety.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.qvety.tenant.SystemContext;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The tenant boundary, enforced at compile output rather than by review.
 *
 * Running without a tenant is the one thing that can quietly undo row-level security, so exactly two
 * callers may do it: the platform package, which is about practices rather than inside one, and
 * AuthService, because login happens before any practice is known and reads the definer functions.
 * Anything else fails the build (part 11, tenant-boundary spec).
 */
@AnalyzeClasses(packages = "com.qvety", importOptions = ImportOption.DoNotIncludeTests.class)
class TenantBoundaryRulesTest {

    @ArchTest
    static final ArchRule systemContextIsConfined = noClasses()
        .that().resideOutsideOfPackage("com.qvety.platform..")
        .and().doNotHaveFullyQualifiedName("com.qvety.auth.AuthService")
        .and().doNotHaveFullyQualifiedName("com.qvety.tenant.SystemContext")
        .and().doNotHaveFullyQualifiedName("com.qvety.tenant.TenantTransactionManager")
        .should().accessClassesThat().belongToAnyOf(SystemContext.class)
        .because("only platform code and login may run without a tenant; the transaction manager reads the flag");

    /** A feature package reaching into platform would put super-admin powers behind a clinic endpoint. */
    @ArchTest
    static final ArchRule featureCodeDoesNotImportPlatform = noClasses()
        .that().resideOutsideOfPackages("com.qvety.platform..", "com.qvety.config..")
        .should().dependOnClassesThat().resideInAPackage("com.qvety.platform..")
        .because("platform is the super-admin side; feature packages must not reach it");
}
