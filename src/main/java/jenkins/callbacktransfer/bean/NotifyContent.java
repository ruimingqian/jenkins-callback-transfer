package jenkins.callbacktransfer.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class NotifyContent implements Serializable {
    private String name;

    private String display_name;

    private String url;

    private Build build;
}