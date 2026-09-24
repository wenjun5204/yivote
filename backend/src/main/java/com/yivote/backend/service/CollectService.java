package com.yivote.backend.service;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.CollectDtos.CollectCreateRequest;
import com.yivote.backend.dto.CollectDtos.CollectDetail;
import com.yivote.backend.dto.CollectDtos.EntryDetail;
import com.yivote.backend.dto.CollectDtos.EntrySubmitRequest;
import com.yivote.backend.dto.CollectDtos.ExportResponse;
import com.yivote.backend.dto.CollectDtos.FieldCreate;
import com.yivote.backend.dto.CollectDtos.FieldDetail;
import com.yivote.backend.dto.VoteDtos.UserBrief;
import com.yivote.backend.entity.CollectEntity;
import com.yivote.backend.entity.CollectEntryEntity;
import com.yivote.backend.entity.CollectFieldEntity;
import com.yivote.backend.entity.UserEntity;
import com.yivote.backend.repository.CollectEntryRepository;
import com.yivote.backend.repository.CollectFieldRepository;
import com.yivote.backend.repository.CollectRepository;
import com.yivote.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收集填表(凑人头):自定义字段表单 → 群友填报 → 自动汇总/导出。
 * 与投票同底座语义:一人一行整行覆盖(可改不可逃),催填走召唤语气。
 */
@Service
public class CollectService {

    private static final int MAX_FIELDS = 6;
    private static final int MAX_OPTIONS = 8;

    private final CollectRepository collectRepository;
    private final CollectFieldRepository fieldRepository;
    private final CollectEntryRepository entryRepository;
    private final UserRepository userRepository;

    public CollectService(CollectRepository collectRepository,
                           CollectFieldRepository fieldRepository,
                           CollectEntryRepository entryRepository,
                           UserRepository userRepository) {
        this.collectRepository = collectRepository;
        this.fieldRepository = fieldRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CollectDetail create(CollectCreateRequest request) {
        String title = trim(request.getTitle());
        if (title.isEmpty()) {
            throw ApiException.param("给收集起个标题吧");
        }
        if (title.length() > 30) {
            throw ApiException.param("标题 30 字以内");
        }
        List<FieldCreate> fields = request.getFields();
        if (fields == null || fields.isEmpty()) {
            throw ApiException.param("至少加一个要收集的字段");
        }
        if (fields.size() > MAX_FIELDS) {
            throw ApiException.param("字段最多 " + MAX_FIELDS + " 个，太多了大家懒得填");
        }

        LocalDateTime now = LocalDateTime.now();
        CollectEntity collect = new CollectEntity();
        collect.setTitle(title);
        collect.setOwnerId(CurrentUser.get());
        collect.setOpenGId(trimToNull(request.getOpenGId()));
        collect.setStatus("OPEN");
        collect.setCreatedAt(now);
        collect.setUpdatedAt(now);
        collect = collectRepository.save(collect);

        int order = 0;
        for (FieldCreate fieldCreate : fields) {
            String label = trim(fieldCreate.getLabel());
            if (label.isEmpty()) {
                throw ApiException.param("有个字段名是空的，补一下再发");
            }
            String kind = fieldCreate.getKind();
            List<String> options = fieldCreate.getOptions();
            if ("CHIP".equals(kind)) {
                if (options == null || options.isEmpty()) {
                    throw ApiException.param("点选字段「" + label + "」要加候选项");
                }
                if (options.size() > MAX_OPTIONS) {
                    throw ApiException.param("候选项最多 " + MAX_OPTIONS + " 个");
                }
            } else if (!"TEXT".equals(kind)) {
                throw ApiException.param("字段「" + label + "」的类型不对");
            }
            CollectFieldEntity field = new CollectFieldEntity();
            field.setCollectId(collect.getId());
            field.setLabel(label);
            field.setKind(kind);
            field.setOptions("CHIP".equals(kind) ? String.join(",", options) : null);
            field.setSortOrder(order++);
            fieldRepository.save(field);
        }
        return detail(collect.getId(), CurrentUser.get());
    }

    @Transactional(readOnly = true)
    public CollectDetail detail(Long collectId, Long userId) {
        CollectEntity collect = requireCollect(collectId);
        return toDetail(collect, userId);
    }

    /** 提交/改填:一人一行整行覆盖 */
    @Transactional
    public CollectDetail submitEntry(Long collectId, EntrySubmitRequest request) {
        CollectEntity collect = requireCollect(collectId);
        if (!"OPEN".equals(collect.getStatus())) {
            throw ApiException.param("这个表已经收过了，看看汇总吧");
        }
        List<CollectFieldEntity> fields = fieldRepository.findByCollectIdOrderBySortOrderAsc(collectId);
        List<String> values = request.getValues();
        if (values == null || values.size() != fields.size()) {
            throw ApiException.param("填写不完整，每项都要填一下");
        }
        for (int i = 0; i < fields.size(); i++) {
            String value = trim(values.get(i));
            if (value.isEmpty()) {
                throw ApiException.param("「" + fields.get(i).getLabel() + "」还没填");
            }
            if (value.length() > 60) {
                throw ApiException.param("「" + fields.get(i).getLabel() + "」太长了，60 字以内");
            }
            if ("CHIP".equals(fields.get(i).getKind())) {
                List<String> options = List.of(fields.get(i).getOptions().split(","));
                if (!options.contains(value)) {
                    throw ApiException.param("「" + value + "」不在候选项里");
                }
            }
        }

        Long userId = CurrentUser.get();
        entryRepository.deleteByCollectIdAndUserId(collectId, userId);
        CollectEntryEntity entry = new CollectEntryEntity();
        entry.setCollectId(collectId);
        entry.setUserId(userId);
        entry.setValuesCsv(escape(values));
        entry.setUpdatedAt(LocalDateTime.now());
        entryRepository.save(entry);

        collect.setUpdatedAt(LocalDateTime.now());
        collectRepository.save(collect);
        return toDetail(collect, userId);
    }

    /** 发起人复制汇总名单 */
    @Transactional(readOnly = true)
    public ExportResponse export(Long collectId) {
        CollectEntity collect = requireCollect(collectId);
        Long userId = CurrentUser.get();
        if (!collect.getOwnerId().equals(userId)) {
            throw ApiException.forbidden("只有发起人能导出名单");
        }
        List<CollectFieldEntity> fields = fieldRepository.findByCollectIdOrderBySortOrderAsc(collectId);
        List<CollectEntryEntity> entries = entryRepository.findByCollectIdOrderByUpdatedAtAsc(collectId);

        StringBuilder sb = new StringBuilder(collect.getTitle()).append('\n');
        int index = 1;
        for (CollectEntryEntity entry : entries) {
            UserEntity user = userRepository.findById(entry.getUserId()).orElse(null);
            String name = user != null ? user.getNickname() : "群友";
            sb.append(index++).append(". ").append(name);
            List<String> values = unescape(entry.getValuesCsv());
            for (int i = 0; i < fields.size() && i < values.size(); i++) {
                sb.append(' ').append(fields.get(i).getLabel()).append(':').append(values.get(i));
            }
            sb.append('\n');
        }
        ExportResponse response = new ExportResponse();
        response.setText(sb.toString());
        return response;
    }

    /* ---------------- 汇总 ---------------- */

    private CollectDetail toDetail(CollectEntity collect, Long userId) {
        CollectDetail detail = new CollectDetail();
        detail.setCollectId(collect.getId());
        detail.setTitle(collect.getTitle());
        detail.setStatus(collect.getStatus());
        detail.setOwner(toBrief(userRepository.findById(collect.getOwnerId()).orElse(null)));

        List<CollectFieldEntity> fields = fieldRepository.findByCollectIdOrderBySortOrderAsc(collect.getId());
        List<FieldDetail> fieldDetails = new ArrayList<>();
        for (CollectFieldEntity field : fields) {
            FieldDetail fd = new FieldDetail();
            fd.setFieldId(field.getId());
            fd.setLabel(field.getLabel());
            fd.setKind(field.getKind());
            fd.setOptions(field.getOptions() == null ? null : List.of(field.getOptions().split(",")));
            fieldDetails.add(fd);
        }
        detail.setFields(fieldDetails);

        List<CollectEntryEntity> entries = entryRepository.findByCollectIdOrderByUpdatedAtAsc(collect.getId());
        List<EntryDetail> entryDetails = new ArrayList<>();
        for (CollectEntryEntity entry : entries) {
            EntryDetail ed = new EntryDetail();
            ed.setUser(toBrief(userRepository.findById(entry.getUserId()).orElse(null)));
            ed.setValues(unescape(entry.getValuesCsv()));
            entryDetails.add(ed);
        }
        detail.setEntries(entryDetails);
        detail.setFilledCount(entryDetails.size());

        entryRepository.findByCollectIdAndUserId(collect.getId(), userId)
                .ifPresent(e -> detail.setMyValues(unescape(e.getValuesCsv())));

        // 发起人视角补位提示(召唤语气,永不质问)
        if (collect.getOwnerId().equals(userId)) {
            detail.setNotFilledHint("表还开着，随时能填；复制提醒话术去微信喊一声，每天最多 1 次");
        }
        return detail;
    }

    private UserBrief toBrief(UserEntity user) {
        if (user == null) {
            return null;
        }
        UserBrief brief = new UserBrief();
        brief.setUserId(user.getId());
        brief.setNickname(user.getNickname());
        brief.setAvatarUrl(user.getAvatarUrl());
        return brief;
    }

    private CollectEntity requireCollect(Long collectId) {
        return collectRepository.findById(collectId)
                .orElseThrow(() -> ApiException.notFound("这个收集不存在，可能已被删除"));
    }

    /* csv 值转义:值内逗号以 \, 转义,防止错位 */
    static String escape(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(trim(values.get(i)).replace(",", "\\,"));
        }
        return sb.toString();
    }

    static List<String> unescape(String csv) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '\\' && i + 1 < csv.length() && csv.charAt(i + 1) == ',') {
                current.append(',');
                i++;
            } else if (c == ',') {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
