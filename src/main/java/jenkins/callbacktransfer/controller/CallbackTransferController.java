package jenkins.callbacktransfer.controller;

import jenkins.callbacktransfer.bean.NotifyContent;
import jenkins.callbacktransfer.service.CallbackTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("kd/cd/jenkins")
@RestController
@Slf4j
public class CallbackTransferController {
    private final CallbackTransferService transferService;

    @Autowired
    public CallbackTransferController(CallbackTransferService transferService) {
        this.transferService = transferService;
    }

    @CrossOrigin
    @RequestMapping("/test")
    public String test() {
        return "测试";
    }

    @CrossOrigin
    @RequestMapping("/yzj/notify")
    public void toYzj(@RequestBody NotifyContent notifyContent) {
        transferService.onNotify(notifyContent);
    }
}
