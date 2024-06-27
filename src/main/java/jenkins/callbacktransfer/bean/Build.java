package jenkins.callbacktransfer.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class Build implements Serializable {
    private String full_url;

    private int number;

    private int queue_id;

    private long timestamp;

    private int duration;

    private String phase;

    private String status;

    private String url;

    private Scm scm;

    private String log;

    private String notes;

    private Artifacts artifacts;
}