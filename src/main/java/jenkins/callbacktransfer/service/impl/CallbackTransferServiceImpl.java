package jenkins.callbacktransfer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jenkins.callbacktransfer.bean.Build;
import jenkins.callbacktransfer.bean.NotifyContent;
import jenkins.callbacktransfer.bean.Scm;
import jenkins.callbacktransfer.service.CallbackTransferService;
import jenkins.callbacktransfer.util.ResourceUtils;
import jenkins.callbacktransfer.yzj.YZJBot;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Primary
@Component
public class CallbackTransferServiceImpl implements CallbackTransferService {
    private static final JsonNode scmNode;
    @Value("${job_prefix}")
    private String jobPrefix;
    @Value("${biz_chop_path}")
    private String bizChopPath;
    @Value("${exclude_prefix}")
    private String excludePrefix;
    @Value("${exclude_chop_path}")
    private String excludeChopPath;
    @Value("${yzj.url}")
    private String notifyUrl;

    public void onNotify(NotifyContent content) {
        YZJBot bot = new YZJBot("Jenkins回调", notifyUrl);
        Build build = content.getBuild();
        if ("SUCCESS".equalsIgnoreCase(build.getStatus())) {
            Scm scm = build.getScm();
            //暂时排除报表模块提交
            if (!allChangesAreFromExcludeModule(scm)) {
                String msg = String.format("%s %s#%s %s%n【分支】%s%n【变更项】%s%n当前构建始于 %s    耗时%s秒",
                        getCulpritsNameExpr(scm),
                        getEnvName(content),
                        build.getNumber(),
                        build.getNotes(),
                        scm.getBranch(),
                        getChangedCodeDesc(scm),
                        deduceStartTime(build.getDuration()),
                        build.getDuration() / 1000);

                bot.notifyByParts(msg, 1500);
            }

        } else if ("FAILURE".equalsIgnoreCase(build.getStatus())) {
            bot.notify(String.format("%s#%s构建失败，请联系管理员！",
                    getEnvName(content),
                    build.getNumber()));
        }
    }

    private String getEnvName(NotifyContent content) {
        return StringUtils.substringAfter(content.getDisplay_name(), jobPrefix).toUpperCase(Locale.ENGLISH);
    }

    private boolean allChangesAreFromExcludeModule(Scm scm) {
        if (StringUtils.isEmpty(excludePrefix)) {
            return false;
        }
        List<String> changes = scm.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return false;
        }
        for (String change : changes) {
            if (!StringUtils.contains(change, excludePrefix)) {
                return false;
            }
        }
        return true;
    }

    private String getChangedCodeDesc(Scm scm) {
        List<String> changes = scm.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return "无";
        }
        return "\n" + scm.getChanges().stream().map(s -> ".." + chopClassPath(s)).distinct().collect(Collectors.joining("\n")) + "\n";
    }

    private String chopClassPath(String classPath) {
        if (StringUtils.startsWith(classPath, bizChopPath)) {
            return StringUtils.substringAfter(classPath, bizChopPath);
        } else if (StringUtils.startsWith(classPath, excludeChopPath)) {
            return StringUtils.substringAfter(classPath, excludeChopPath);
        } else {
            return classPath;
        }
    }

    @SneakyThrows
    private static String getCulpritsNameExpr(Scm scm) {
        List<String> culprits = scm.getCulprits();
        StringBuilder sb = new StringBuilder(culprits.size());

        for (String culprit : culprits) {
            JsonNode node = scmNode.get(culprit);
            if (node != null) {
                String name = node.textValue();
                if (StringUtils.isNotBlank(name)) {
                    sb.append('@').append(name.trim()).append(' ');
                }
            }
        }
        return StringUtils.removeEnd(sb.toString(), " ");
    }

    private static String deduceStartTime(int duration) {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis() - duration);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return localDateTime.format(formatter);
    }

    static {
        try {
            String str = ResourceUtils.readFileStr("account.json");
            scmNode = new ObjectMapper().readTree(str);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
