package top.mothership.cb3.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cb3.task.ImportTask;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private ImportTask importTask;

    @PostMapping("/trigger")
    public String triggerImport() {
        try {
            importTask.importUserInfo();
            return "Import task triggered successfully.";
        } catch (Exception e) {
            return "Failed to trigger import task: " + e.getMessage();
        }
    }
}