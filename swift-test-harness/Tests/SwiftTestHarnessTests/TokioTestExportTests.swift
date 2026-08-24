import Testing
import TokioTest

@Suite("TokioTest Swift Export Suite")
struct TokioTestExportTests {
    @Test("Swift module loads cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true), "TokioTest swift module imported cleanly")
    }

    @Test("TokioTestLib module info is accessible")
    func moduleInfo() {
        #expect(TokioTestLib.shared.CRATE_NAME == "tokiotest")
        #expect(TokioTestLib.shared.MODULE_NAME == "tokiotest")
    }
}
