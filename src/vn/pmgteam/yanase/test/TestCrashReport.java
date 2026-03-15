package vn.pmgteam.yanase.test;

import vn.pmgteam.yanase.util.CrashReport;

public class TestCrashReport {

    public static void main(String[] args) {
        System.out.println("[Test] Khoi chay he thong kiem tra CrashReport...");
        
        // GIA LAP: He thong dang cham nguong 97.5% RAM
        double mockRamUsage = 97.5;
        
        System.out.println("[Test] Dang gia lap tinh trang: " + mockRamUsage + "% RAM");

        // Kiem tra logic kich hoat
        if (mockRamUsage >= 97.0) {
            System.err.println("[Test] CANH BAO: RAM da cham nguong tu than!");
            
            // Goi ham make de sinh file log voi ma loi Emergency Exit
            // Day la luc "di chuc" duoc viet ra
            CrashReport.make(CrashReport.ERR_EMERGENCY_EXIT, mockRamUsage);
            
            System.out.println("[Test] Crash Report da duoc khoi tao. Kiem tra thu muc du an.");
        } else {
            System.out.println("[Test] RAM van trong nguong an toan (gia lap).");
        }
        
        System.out.println("[Test] Ket thuc chuong trinh kiem tra.");
    }
}