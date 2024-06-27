package jenkins.callbacktransfer.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Scm implements Serializable {
    private String url;

    private String branch;

    private String commit;

    private List<String> changes;

    private List<String> culprits;
}