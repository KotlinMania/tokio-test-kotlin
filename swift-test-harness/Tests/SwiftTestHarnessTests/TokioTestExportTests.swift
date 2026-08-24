#if canImport(Testing)
import Testing
import TokioTest

@Suite("TokioTest Swift Export Smoke Test")
struct TokioTestExportTests {
    @Test("Swift module loads")
    func swiftModuleLoads() throws {
        #expect(true)
    }
}
#elseif canImport(XCTest)
import XCTest
import TokioTest

final class TokioTestExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "TokioTest swift module imported cleanly")
    }
}
#endif
