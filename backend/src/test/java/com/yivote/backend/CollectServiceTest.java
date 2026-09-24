package com.yivote.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yivote.backend.common.ApiException;
import com.yivote.backend.common.CurrentUser;
import com.yivote.backend.dto.AuthDtos.LoginRequest;
import com.yivote.backend.dto.AuthDtos.LoginResponse;
import com.yivote.backend.dto.CollectDtos.CollectCreateRequest;
import com.yivote.backend.dto.CollectDtos.CollectDetail;
import com.yivote.backend.dto.CollectDtos.EntrySubmitRequest;
import com.yivote.backend.dto.CollectDtos.ExportResponse;
import com.yivote.backend.dto.CollectDtos.FieldCreate;
import com.yivote.backend.service.AuthService;
import com.yivote.backend.service.CollectService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 收集填表链路:发起 → 填报/改填 → 汇总导出 */
@SpringBootTest
class CollectServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private CollectService collectService;

    private Long ownerId;
    private Long memberA;
    private Long memberB;

    @BeforeEach
    void setUp() {
        ownerId = login("collect-owner-" + System.nanoTime());
        memberA = login("collect-a-" + System.nanoTime());
        memberB = login("collect-b-" + System.nanoTime());
    }

    private Long login(String code) {
        LoginRequest req = new LoginRequest();
        req.setCode(code);
        LoginResponse resp = authService.login(req, null);
        return resp.getUserId();
    }

    private CollectDetail createSignupForm(Long userId) {
        CurrentUser.set(userId);
        CollectCreateRequest request = new CollectCreateRequest();
        request.setTitle("球队新赛季报名");
        request.setOpenGId("og-test-" + System.nanoTime());
        FieldCreate name = new FieldCreate();
        name.setLabel("手机号");
        name.setKind("TEXT");
        FieldCreate size = new FieldCreate();
        size.setLabel("球衣尺码");
        size.setKind("CHIP");
        size.setOptions(List.of("S", "M", "L", "XL"));
        request.setFields(List.of(name, size));
        return collectService.create(request);
    }

    @Test
    void createFieldsAndChipOptions() {
        CollectDetail created = createSignupForm(ownerId);
        assertNotNull(created.getCollectId());
        assertEquals(2, created.getFields().size());
        assertEquals(List.of("S", "M", "L", "XL"), created.getFields().get(1).getOptions());
        assertEquals("OPEN", created.getStatus());
    }

    @Test
    void fillThenRefillOverwrites() {
        CollectDetail created = createSignupForm(ownerId);

        CurrentUser.set(memberA);
        EntrySubmitRequest first = new EntrySubmitRequest();
        first.setValues(List.of("13800001111", "M"));
        collectService.submitEntry(created.getCollectId(), first);

        // 改填:尺码变了
        EntrySubmitRequest second = new EntrySubmitRequest();
        second.setValues(List.of("13800001111", "XL"));
        CollectDetail after = collectService.submitEntry(created.getCollectId(), second);

        assertEquals(1, after.getFilledCount(), "改填不新增行");
        assertEquals(List.of("13800001111", "XL"), after.getMyValues());
        assertEquals(1, after.getEntries().size());
    }

    @Test
    void invalidEntryValuesRejected() {
        CollectDetail created = createSignupForm(ownerId);
        CurrentUser.set(memberA);

        EntrySubmitRequest shortEntry = new EntrySubmitRequest();
        shortEntry.setValues(List.of("13800001111"));
        ApiException ex1 = assertThrows(ApiException.class,
                () -> collectService.submitEntry(created.getCollectId(), shortEntry));
        assertEquals("40000", ex1.getCode());

        EntrySubmitRequest badChip = new EntrySubmitRequest();
        badChip.setValues(List.of("13800001111", "XXL"));
        ApiException ex2 = assertThrows(ApiException.class,
                () -> collectService.submitEntry(created.getCollectId(), badChip));
        assertTrue(ex2.getMessage().contains("不在候选项"));
    }

    @Test
    void exportByOwnerOnly() {
        CollectDetail created = createSignupForm(ownerId);
        CurrentUser.set(memberA);
        EntrySubmitRequest entry = new EntrySubmitRequest();
        entry.setValues(List.of("13800001111", "L"));
        collectService.submitEntry(created.getCollectId(), entry);
        CurrentUser.set(memberB);
        EntrySubmitRequest entryB = new EntrySubmitRequest();
        entryB.setValues(List.of("13900002222", "S"));
        collectService.submitEntry(created.getCollectId(), entryB);

        CurrentUser.set(ownerId);
        ExportResponse export = collectService.export(created.getCollectId());
        assertTrue(export.getText().contains("手机号:13800001111"));
        assertTrue(export.getText().contains("尺码:S"));

        CurrentUser.set(memberA);
        ApiException denied = assertThrows(ApiException.class,
                () -> collectService.export(created.getCollectId()));
        assertEquals("40300", denied.getCode());
    }
}
