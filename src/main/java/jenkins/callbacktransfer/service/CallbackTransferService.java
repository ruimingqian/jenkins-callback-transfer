package jenkins.callbacktransfer.service;

import jenkins.callbacktransfer.bean.NotifyContent;

public interface CallbackTransferService {
    void onNotify(NotifyContent notifyContent);
}
