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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class CallbackTransferServiceSDIGImpl implements CallbackTransferService {
    private static final String BIZ_PREFIX = "code/kd-cd-sdjt/kd-cd-sdjt-biz/src/kd/cd/sdjt/biz";
    private static final String RPT_PREFIX = "code/kd-cd-sdjt/kd-cd-sdjt-rpt-budget/src/main/java/kd/cd/sdjt/biz";
    private static final JsonNode scmNode;
    @Value("${yzj.url}")
    private String notifyUrl;

    public void onNotify(NotifyContent content) {
        YZJBot bot = new YZJBot("Jenkins回调", notifyUrl);
        Build build = content.getBuild();
        if ("SUCCESS".equalsIgnoreCase(build.getStatus())) {
            Scm scm = build.getScm();
            //暂时排除报表模块提交
            if (!allChangesAreFromRptMoudle(scm)) {
                String msg = String.format("%s %s#%s %s%n【分支】%s%n【变更项】%s%n当前构建始于 %s    耗时%s秒",
                        getCulpritsNameExpr(scm),
                        getEnvName(content),
                        build.getNumber(),
                        build.getNotes(),
                        scm.getBranch(),
                        getChangedCodeDesc(scm),
                        caulStartTime(build.getDuration()),
                        build.getDuration() / 1000);

                bot.notifyByParts(msg, 1500);
            }

        } else if ("FAILURE".equalsIgnoreCase(build.getStatus())) {
            bot.notify(String.format("%s#%s构建失败，请联系管理员！",
                    getEnvName(content),
                    build.getNumber()));
        }
    }

    private static String getEnvName(NotifyContent content) {
        return StringUtils.substringAfter(content.getDisplay_name(), "kd-cd-sdjt-biz-").toUpperCase(Locale.ENGLISH);
    }

    private boolean allChangesAreFromRptMoudle(Scm scm) {
        List<String> changes = scm.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return false;
        }
        for (String change : changes) {
            if (!StringUtils.contains(change, "kd-cd-sdjt-rpt-budget")) {
                return false;
            }
        }
        return true;
    }

    private static String getChangedCodeDesc(Scm scm) {
        List<String> changes = scm.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return "无";
        }
        return "\n" + scm.getChanges().stream().map(s -> ".." + chopClassPath(s)).distinct().collect(Collectors.joining("\n")) + "\n";
    }

    private static String chopClassPath(String classPath) {
        if (StringUtils.startsWith(classPath, BIZ_PREFIX)) {
            return StringUtils.substringAfter(classPath, BIZ_PREFIX);
        } else if (StringUtils.startsWith(classPath, RPT_PREFIX)) {
            return StringUtils.substringAfter(classPath, RPT_PREFIX);
        } else {
            return classPath;
        }
    }

    @SneakyThrows
    private static String getCulpritsNameExpr(Scm scm) {
        List<String> culprits = scm.getCulprits();
        StringBuilder sb = new StringBuilder(culprits.size());

        for (String culprit : culprits) {
            String name = scmNode.get(culprit).textValue();
            if (StringUtils.isNotBlank(name)) {
                sb.append('@').append(name.trim()).append(' ');
            }
        }
        return StringUtils.removeEnd(sb.toString(), " ");
    }

    private static String caulStartTime(int duration) {
        Instant instant = Instant.ofEpochMilli(System.currentTimeMillis() - duration);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return localDateTime.format(formatter);
    }

    static {
        try {
            String str = ResourceUtils.readFileStr("scmaccount.json");
            scmNode = new ObjectMapper().readTree(str);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
