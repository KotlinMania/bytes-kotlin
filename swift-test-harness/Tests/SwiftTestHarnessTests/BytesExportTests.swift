import XCTest
import Bytes

// Smoke test for the Kotlin → Swift Export → SPM → swift test pipeline.
//
// The file's mere existence and successful compilation prove three layers
// of the pipeline:
//
//   1. `embedSwiftExportForXcode` produced `Bytes.swiftmodule/`
//      and the supporting KotlinRuntimeSupport / ExportedKotlinPackages /
//      KotlinRuntime swiftmodule bundles. If any of them were missing,
//      `import Bytes` above would fail at compile time.
//
//   2. The static archive `libBytes.a` (produced by the
//      `linkSwiftExportBinaryDebugStaticMacosArm64` and
//      `mergeMacosDebugSwiftExportLibraries` tasks) supplied every
//      `__root____*` and `KotlinError`-related symbol the Swift modules
//      reference. If the archive were missing or empty, this test
//      executable would fail to link with "undefined symbols for
//      architecture arm64".
//
//   3. The Kotlin `swiftExport { moduleName = "Bytes" }` and
//      `flattenPackage = "io.github.kotlinmania.bytes"` configuration in
//      build.gradle.kts produced a module name that's both syntactically
//      valid as a Swift identifier and reachable from this Package.swift
//      via the `BytesLibrary` product.
//
// Add more meaningful per-API tests below as the Swift Export surface
// grows. For now the import + a single passing assertion is the
// canary that the pipeline is green for this repo.
final class BytesExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Bytes swift module imported cleanly")
    }
}
