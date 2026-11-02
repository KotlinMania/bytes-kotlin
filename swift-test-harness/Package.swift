// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "SwiftTestHarness",
    platforms: [
        .macOS(.v15),
    ],
    dependencies: [
        .package(name: "Bytes", path: "../build/SPMPackage/macosArm64/Debug")
    ],
    targets: [
        .testTarget(
            name: "SwiftTestHarnessTests",
            dependencies: [
                .product(name: "BytesLibrary", package: "Bytes")
            ],
            // Preserve the copied template file on disk; run the project tests.
            exclude: ["ExampleTests.swift"],
            linkerSettings: [
                .unsafeFlags([
                    "-L", "../build/swift-test",
                    "-lBytes",
                ]),
            ]
        ),
    ],
    swiftLanguageModes: [.v6]
)
