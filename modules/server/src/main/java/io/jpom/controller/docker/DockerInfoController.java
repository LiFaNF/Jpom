/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.docker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.multipart.MultipartFileBuilder;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.docker.DockerSwarmInfoMode;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.system.ServerConfig;
import io.jpom.util.CompressionFileUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker")
@Slf4j
public class DockerInfoController extends BaseServerController {

    private final DockerInfoService dockerInfoService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final ServerConfig serverConfig;

    public DockerInfoController(DockerInfoService dockerInfoService,
                                DockerSwarmInfoService dockerSwarmInfoService,
                                ServerConfig serverConfig) {
        this.dockerInfoService = dockerInfoService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.serverConfig = serverConfig;
    }

    /**
     * @return json
     */
    @GetMapping(value = "api-versions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<JSONObject>> apiVersions() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        List<JSONObject> data = (List<JSONObject>) plugin.execute("apiVersions");
        return JsonMessage.success("", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<DockerInfoModel>> list() {
        // load list with page
        PageResultDto<DockerInfoModel> resultDto = dockerInfoService.listPage(getRequest());
        resultDto.each(this::checkCertPath);
        return JsonMessage.success("", resultDto);
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param dockerInfoModel docker
     * @return true 证书文件存在
     */
    private boolean checkCertPath(DockerInfoModel dockerInfoModel) {
        if (dockerInfoModel == null) {
            return false;
        }
        if (dockerInfoModel.getCertExist() != null && dockerInfoModel.getCertExist()) {
            return true;
        }
        String certPath = dockerInfoModel.generateCertPath();
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        try {
            boolean execute = (boolean) plugin.execute("certPath", "certPath", certPath);
            dockerInfoModel.setCertExist(execute);
            return execute;
        } catch (Exception e) {
            log.error("检查 docker 证书异常", e);
            return false;
        }
    }

    /**
     * 接收前端参数
     *
     * @param certPathFile 证书保存临时文件夹
     * @return model
     * @throws Exception 异常
     */
    private DockerInfoModel takeOverModel(File certPathFile) throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        String name = getParameter("name");
        Assert.hasText(name, "请填写 名称");
        String host = getParameter("host");
        String id = getParameter("id");
        String tlsVerifyStr = getParameter("tlsVerify");
        String apiVersion = getParameter("apiVersion");
        String tagsStr = getParameter("tags", StrUtil.EMPTY);
        String registryUrl = getParameter("registryUrl");
        String registryUsername = getParameter("registryUsername");
        String registryPassword = getParameter("registryPassword");
        String registryEmail = getParameter("registryEmail");
        int heartbeatTimeout = getParameterInt("heartbeatTimeout", -1);
        boolean tlsVerify = Convert.toBool(tlsVerifyStr, false);
        //
        boolean certExist = false;
        if (tlsVerify) {
            // 如果是创建就必须上传证书
            MultipartFileBuilder multipart = null;
            try {
                multipart = createMultipart();
            } catch (Exception e) {
                DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
                certExist = this.checkCertPath(dockerInfoModel);
                Assert.state(certExist, "请上传证书文件");
            }
            if (multipart != null) {
                String absolutePath = FileUtil.getAbsolutePath(certPathFile);
                multipart.setSavePath(absolutePath).addFieldName("file").setUseOriginalFilename(true);
                String localPath = multipart.setFileExt(StringUtil.PACKAGE_EXT).save();
                // 解压
                File file = new File(localPath);
                CompressionFileUtil.unCompress(file, certPathFile);
                boolean ok = (boolean) plugin.execute("certPath", "certPath", absolutePath);
                Assert.state(ok, "证书信息不正确,证书压缩包里面必须包含：ca.pem、key.pem、cert.pem");
                certExist = true;
            }
        }
        boolean ok = (boolean) plugin.execute("host", "host", host);
        Assert.state(ok, "请填写正确的 host");
        //
        Assert.state(!StrUtil.contains(tagsStr, StrUtil.COLON), "标签不能包含 ：");
        List<String> tagList = StrUtil.splitTrim(tagsStr, StrUtil.COMMA);
        String newTags = CollUtil.join(tagList, StrUtil.COLON, StrUtil.COLON, StrUtil.COLON);
        // 验证重复
        String workspaceId = dockerInfoService.getCheckUserWorkspace(getRequest());
        Entity entity = Entity.create();
        entity.set("host", host);
        entity.set("workspaceId", workspaceId);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = dockerInfoService.exists(entity);
        Assert.state(!exists, "对应的 docker 已经存在啦");
        //
        DockerInfoModel.DockerInfoModelBuilder builder = DockerInfoModel.builder();
        builder.heartbeatTimeout(heartbeatTimeout).apiVersion(apiVersion).host(host).name(name)
            .tlsVerify(tlsVerify).certExist(certExist).tags(newTags)
            .registryUrl(registryUrl).registryUsername(registryUsername).registryPassword(registryPassword).registryEmail(registryEmail);
        //
        DockerInfoModel build = builder.build();
        build.setId(id);
        return build;
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> edit(String id, String host) throws Exception {
        // 保存路径
        File tempPath = serverConfig.getUserTempPath();
        File savePath = FileUtil.file(tempPath, "docker", SecureUtil.sha1(host));
        DockerInfoModel dockerInfoModel = this.takeOverModel(savePath);
        boolean certExist = dockerInfoModel.getCertExist();
        if (StrUtil.isEmpty(id)) {
            // 创建
            if (dockerInfoModel.getTlsVerify()) {
                Assert.state(certExist, "请上传证书文件");
            }
            this.check(dockerInfoModel, certExist, savePath);
            // 默认正常
            dockerInfoModel.setStatus(1);
            dockerInfoService.insert(dockerInfoModel);
        } else {
            this.check(dockerInfoModel, certExist, savePath);
            dockerInfoService.updateById(dockerInfoModel, getRequest());
        }
        //
        return JsonMessage.success("操作成功");
    }

    private void check(DockerInfoModel dockerInfoModel, boolean certExist, File savePath) throws Exception {
        // 移动证书
        if (certExist) {
            if (FileUtil.isDirectory(savePath) && FileUtil.isNotEmpty(savePath)) {
                String generateCertPath = dockerInfoModel.generateCertPath();
                FileUtil.moveContent(savePath, FileUtil.file(generateCertPath), true);
            }
        }
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("closeBefore", true);
        boolean ok = (boolean) plugin.execute("ping", parameter);
        Assert.state(ok, "无法连接 docker 请检查 host 或者 TLS 证书");
        // 检查授权
        String registryUrl = dockerInfoModel.getRegistryUrl();
        if (StrUtil.isNotEmpty(registryUrl)) {
            DockerInfoModel oldInfoModel = dockerInfoService.getByKey(dockerInfoModel.getId(), false);
            String registryPassword = Optional.ofNullable(dockerInfoModel.getRegistryPassword()).orElseGet(() -> Optional.ofNullable(oldInfoModel).map(DockerInfoModel::getRegistryPassword).orElse(null));
            Assert.hasText(registryPassword, "仓库密码不能为空");
            parameter.put("closeBefore", true);
            parameter.put("registryPassword", registryPassword);
            try {
                JSONObject jsonObject = (JSONObject) plugin.execute("testAuth", parameter);
                log.info("{}", jsonObject);
            } catch (Exception e) {
                log.warn("仓库授权信息错误", e);
                throw new IllegalArgumentException("仓库账号或者密码错误：" + e.getMessage());
            }
        }
    }


    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem String id) throws Exception {
        DockerInfoModel infoModel = dockerInfoService.getByKey(id, getRequest());
        if (infoModel != null) {
            // 检查是否为最后一个 docker 需要删除证书文件
            DockerInfoModel dockerInfoModel = new DockerInfoModel();
            dockerInfoModel.setHost(infoModel.getHost());
            long count = dockerInfoService.count(dockerInfoService.dataBeanToEntity(dockerInfoModel));
            if (count <= 1) {
                // 删除文件
                FileUtil.del(infoModel.generateCertPath());
            }
            dockerInfoService.delByKey(id);
        }
        return JsonMessage.success("删除成功");
    }

    @GetMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> info(@ValidatorItem String id) throws Exception {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        JSONObject info = plugin.execute("info", dockerInfoModel.toParameter(), JSONObject.class);
        return JsonMessage.success("", info);
    }

    /**
     * 强制退出集群
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "swarm-leave-force", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> leaveForce(@ValidatorItem String id) throws Exception {
        //
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setDockerId(id);
        long count = dockerSwarmInfoService.count(dockerSwarmInfoMode);
        Assert.state(count <= 0, "需要先解绑集群才能强制退出集群");
        //
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("force", true);
        plugin.execute("leaveSwarm", parameter, JSONObject.class);
        //
        dockerInfoService.unbind(id);
        return new JsonMessage<>(200, "强制解绑成功");
    }

    @GetMapping(value = "try-local-docker", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> tryLocalDocker() {
        try {
            String workspaceId = dockerInfoService.getCheckUserWorkspace(getRequest());
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            String dockerHost = (String) plugin.execute("testLocal", new HashMap<>(1));
            Entity entity = Entity.create();
            entity.set("host", dockerHost);
            entity.set("workspaceId", workspaceId);
            boolean exists = dockerInfoService.exists(entity);
            if (exists) {
                return new JsonMessage<>(405, "已经存在本地 docker 信息啦，不要重复添加");
            }
            DockerInfoModel.DockerInfoModelBuilder builder = DockerInfoModel.builder();
            builder.host(dockerHost).name("localhost").status(1);
            DockerInfoModel dockerInfoModel = builder.build();
            dockerInfoModel.setWorkspaceId(workspaceId);
            dockerInfoService.insert(dockerInfoModel);
            return new JsonMessage<>(200, "自动探测到本地 docker 并且自动添加：" + dockerHost);
        } catch (Throwable e) {
            log.error("探测本地 docker 异常", e);
            return new JsonMessage<>(500, "探测本地 docker 异常：" + e.getMessage());
        }
    }

    @PostMapping(value = "prune", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> prune(@ValidatorItem String id, @ValidatorItem String pruneType, String labels, String until, String dangling) throws Exception {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = dockerInfoModel.toParameter();
        parameter.put("pruneType", pruneType);
        parameter.put("labels", labels);
        parameter.put("until", until);
        parameter.put("dangling", dangling);
        //
        Long spaceReclaimed = plugin.execute("prune", parameter, Long.class);
        spaceReclaimed = ObjectUtil.defaultIfNull(spaceReclaimed, 0L);
        return JsonMessage.success("修剪完成,总回收空间：" + FileUtil.readableFileSize(spaceReclaimed));
    }
}