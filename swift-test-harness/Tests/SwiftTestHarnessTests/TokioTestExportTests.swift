import Testing
import TokioTest

@Suite("TokioTest Swift Export Suite")
struct TokioTestExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "TokioTest swift module imported cleanly")
    }
}
