package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfficialFrameSyncServiceTest {
    private static final String BASE_URL = "https://www.streetfighter.com/6/zh-hans/character";

    @Test
    void parsesEnglishCharacterNameFromProfilePage() {
        String html = """
                <title data-next-head="">Yasmine | STREET FIGHTER 6 | CAPCOM</title>
                <h1><span><desc>YEAR 4</desc></span><svg><desc>YASMINE</desc></svg></h1>
                """;

        assertThat(OfficialFrameSyncService.parseOfficialName(html)).isEqualTo("YASMINE");
    }

    @Test
    void parsesCapcomCharacterSlugsWithHyphens() {
        String html = """
                <a href="/6/zh-hans/character/chun-li">CHUN-LI</a>
                <a href="/6/zh-hans/character/e-honda/frame">E.HONDA</a>
                <a href="/6/zh-hans/character/ingrid?x=1">INGRID</a>
                """;

        assertThat(OfficialFrameSyncService.parseOfficialCharacterSlugs(html))
                .containsExactly("chun-li", "e-honda", "ingrid");
    }

    @Test
    void normalizesOfficialJsonCommandsWithoutLosingIconSemantics() {
        assertThat(OfficialFrameSyncService.capcomCommandInput("236 + LPMPHP", "classic"))
                .isEqualTo("236 + PP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("214 + LKMKHK", "classic"))
                .isEqualTo("214 + KK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("23626 | 26236 + LP", "classic"))
                .isEqualTo("236236 + LP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("2ホールド + 8 + HK", "classic"))
                .isEqualTo("CHARGE2 + 8 + HK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("4溜め6 + LP", "classic"))
                .isEqualTo("CHARGE4 + 6 + LP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("2 溜\nめ 8 + HK", "classic"))
                .isEqualTo("CHARGE2 + 8 + HK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("2回転 + SP", "modern"))
                .isEqualTo("720 + SP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("（近距離で）N or 6 + 弱中", "modern"))
                .isEqualTo("NEAR 5 / 6 + MLMM");
        assertThat(OfficialFrameSyncService.capcomCommandInput("214 + 攻撃二つ", "modern"))
                .isEqualTo("214 + ATKATK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("236 + LP / MP / HP", "classic"))
                .isEqualTo("236 + LP / MP / HP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("LK > LK > LK", "classic"))
                .isEqualTo("LK > LK > LK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("4 + LP", "classic"))
                .isEqualTo("4 + LP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("（近距離で）1回転 + LP", "classic"))
                .isEqualTo("NEAR 360 + LP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("（近距離で）2回転 + LP | MP | HP", "classic"))
                .isEqualTo("NEAR 720 + LP / MP / HP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("（ヘッドプレス後に）LP | MP | HP", "classic"))
                .isEqualTo("FOLLOWUP LP / MP / HP");
        assertThat(OfficialFrameSyncService.capcomCommandInput("(前方ステップ中に)214 + LK", "classic"))
                .isEqualTo("DURING_MOVE 214 + LK");
        assertThat(OfficialFrameSyncService.capcomCommandInput("※SA1発動後、ボタンホールド中に打撃を受ける", "classic"))
                .isEqualTo("ON_STRIKE");
    }

    @Test
    void unescapesJsEncodedJsonQuotesAndNewlinesExactlyOnce() {
        String escapedQuote = "\\".repeat(3) + "\"";
        String escapedNewline = "\\".repeat(2) + "n";
        String encoded = "{\"translation\":\"" + escapedQuote
                + "With Classic Controls" + escapedNewline + "Damage" + escapedQuote + "\"}";

        assertThat(OfficialFrameSyncService.unescapeJsString(encoded))
                .isEqualTo("{\"translation\":\"\\\"With Classic Controls\\nDamage\\\"\"}");
    }

    @Test
    void syncOneImportsCapcomFrameDataFromClassicInputIcons() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/aki/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">Standing Light Punch</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" />L</p></td><td>4</td><td><p>4-6</p></td><td>7</td><td>5</td><td>-1</td><td><span>C</span></td><td>300 <span></span></td><td><ul><li>Starter scaling 20%</li></ul></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td><p>Can be rapid canceled</p></td></tr>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">Crouching Light Punch</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/key-d.png" alt="" /><img src="/6/assets/images/common/controller/key-plus.png" alt="" /><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" />L</p></td><td>5</td><td>5-7</td><td>8</td><td>3</td><td>-2</td><td>-</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td></tr>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">L Fireball</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/key-d.png" alt="" /><img src="/6/assets/images/common/controller/key-dr.png" alt="" /><img src="/6/assets/images/common/controller/key-r.png" alt="" /><img src="/6/assets/images/common/controller/key-plus.png" alt="" /><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" />L</p></td><td>13</td><td>13-15</td><td>40</td><td>0</td><td>-6</td><td>-</td><td>600</td><td></td><td>500</td><td>-1000</td><td>-2000</td><td>500</td><td>Projectile</td><td></td></tr>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">Air Punch</span><p class="frame_classic___gpLR">(During a jump)<img src="/6/assets/images/common/controller/icon_punch_m.png" alt="" /></p></td><td>6</td><td>6-8</td><td>3 frame(s) after landing</td><td>4</td><td>-</td><td>-</td><td>500</td><td></td><td>500</td><td>-1000</td><td>-2000</td><td>500</td><td>High</td><td></td></tr>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">Target Combo</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_m.png" alt="" /><img src="/6/assets/images/common/controller/arrow_3.png" alt="" /><img src="/6/assets/images/common/controller/icon_kick_m.png" alt="" /></p></td><td>8</td><td>8-10</td><td>20</td><td>2</td><td>-4</td><td>-</td><td>700</td><td></td><td>500</td><td>-1000</td><td>-2000</td><td>500</td><td>High</td><td></td></tr>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span class="frame_arts__ZU5YI">Any Strike</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch.png" alt="" /><img src="/6/assets/images/common/controller/key-or.png" alt="" /><img src="/6/assets/images/common/controller/icon_kick.png" alt="" /></p></td><td>10</td><td>10-12</td><td>25</td><td>2</td><td>-6</td><td>-</td><td>800</td><td></td><td>500</td><td>-1000</td><td>-2000</td><td>500</td><td>High</td><td></td></tr>
                                </tbody></table>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("aki", "A.K.I."));

        assertThat(result.success()).isTrue();
        assertThat(result.importedCount()).isEqualTo(6);
        assertThat(insertedFrames).hasSize(6);
        assertThat(insertedFrames.get(0)).satisfies(frame -> {
            assertThat(frame.getMoveName()).isEqualTo("LP");
            assertThat(frame.getStartup()).isEqualTo("4");
            assertThat(frame.getActive()).isEqualTo("4-6");
            assertThat(frame.getRecovery()).isEqualTo("7");
            assertThat(frame.getOnHit()).isEqualTo("5");
            assertThat(frame.getOnBlock()).isEqualTo("-1");
            assertThat(frame.getDamage()).isEqualTo("300");
            assertThat(frame.getCancel()).isEqualTo("C");
            assertThat(frame.getComboScaling()).isEqualTo("Starter scaling 20%");
            assertThat(frame.getDriveGainOnHit()).isEqualTo("250");
            assertThat(frame.getProperties()).isEqualTo("H");
            assertThat(frame.getMiscellaneous()).isEqualTo("Can be rapid canceled");
            assertThat(frame.getSourceLang()).isEqualTo("capcom");
        });
        assertThat(insertedFrames.get(1).getMoveName()).isEqualTo("2 + LP");
        assertThat(insertedFrames.get(2).getMoveName()).isEqualTo("236 + LP");
        assertThat(insertedFrames.get(3).getMoveName()).isEqualTo("JMP");
        assertThat(insertedFrames.get(4).getMoveName()).isEqualTo("MP > MK");
        assertThat(insertedFrames.get(5).getMoveName()).isEqualTo("P / K");
        verify(characterMapper).insert(org.mockito.ArgumentMatchers.<CharacterInfo>argThat(character ->
                "A.K.I.".equals(character.getName())
                        && "aki".equals(character.getOfficialSlug())
                        && expectedUrl.equals(character.getOfficialFrameUrl())
        ));
    }

    @Test
    void syncOneImportsCompleteClassicAndModernIconSequencesFromRenderedTable() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody><tr>
                                  <td class="frame_fixed_m__icTnd frame_skill__tLJuM">
                                    <span>Complete Input</span>
                                    <p class="frame_classic___gpLR">
                                      <img src="/6/assets/images/common/controller/key-ul.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/icon_punch_h.png" />
                                      <img src="/6/assets/images/common/controller/key-or.png" />
                                      <img src="/6/assets/images/common/controller/key-circle.png" />
                                    </p>
                                    <p class="frame_modern__smvDf">
                                      <img src="/6/assets/images/common/controller/key-dc.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/modern_auto.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/modern_l.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/modern_sp.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/modern_dl.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/modern_dp.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/icon_throw.png" />
                                      <img src="/6/assets/images/common/controller/key-plus.png" />
                                      <img src="/6/assets/images/common/controller/key-barrage.png" />
                                    </p>
                                  </td>
                                  <td>4</td><td>4-6</td><td>7</td><td>5</td><td>-1</td><td>C</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td>
                                </tr></tbody></table>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(insertedFrames).extracting(FrameData::getControlType)
                .containsExactly("classic", "modern");
        assertThat(insertedFrames).extracting(FrameData::getDisplayOrder)
                .containsExactly(1, 1);
        assertThat(insertedFrames).extracting(FrameData::getMoveName)
                .containsExactly("7 + HP / 360", "CHARGE2 + AUTO + ML + SP + DI + DP + THROW + HD");
    }

    @Test
    void syncOneImportsClassicAndModernFrameDataFromOfficialJson() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <script>let k=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P","command":"LP","command_modern":"AUTO + 弱","attribute":"上","damage":"300","combo_correct":["始動補正20%"],"drive_gauge_gain_hit":"250","drive_gauge_lose_dguard":"-500","drive_gauge_lose_punish":"-2000","sa_gauge_gain":"300","web_cancel":"C","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5","note":["連打キャンセル対応"],"translation":"Can be rapid canceled"},{"webId":"101","skill":"立ち強P","command":"HP","command_modern":null,"attribute":"上","damage":"900","startup_frame":"12","active_frame":"12-15","recovery_frame":"20","block_frame":"-5","hit_frame":"3"}]}')</script>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(result.importedCount()).isEqualTo(3);
        assertThat(insertedFrames).hasSize(3);
        assertThat(insertedFrames).extracting(FrameData::getDisplayOrder)
                .containsExactly(1, 1, 2);
        assertThat(insertedFrames.get(0)).satisfies(frame -> {
            assertThat(frame.getControlType()).isEqualTo("classic");
            assertThat(frame.getMoveName()).isEqualTo("LP");
            assertThat(frame.getProperties()).isEqualTo("H");
            assertThat(frame.getMiscellaneous()).contains("Can be rapid canceled");
        });
        assertThat(insertedFrames.get(1)).satisfies(frame -> {
            assertThat(frame.getControlType()).isEqualTo("modern");
            assertThat(frame.getMoveName()).isEqualTo("AUTO + ML");
        });
        assertThat(insertedFrames.get(2)).satisfies(frame -> {
            assertThat(frame.getControlType()).isEqualTo("classic");
            assertThat(frame.getMoveName()).isEqualTo("HP");
        });
    }

    @Test
    void syncOneUpdatesOfficialMoveNotationButPreservesManualOverrides() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <script>let k=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P","command":"LP","command_modern":"弱","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5"}]}')</script>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        CharacterInfo existingCharacter = new CharacterInfo();
        existingCharacter.setId(99L);
        existingCharacter.setName("INGRID");
        existingCharacter.setOfficialSlug("ingrid");
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(existingCharacter);
        FrameData classic = new FrameData();
        classic.setId(1L);
        classic.setCharacterId(99L);
        classic.setControlType("classic");
        classic.setDisplayOrder(1);
        classic.setMoveName("手动中文轻拳");
        classic.setManualOverride(true);
        classic.setSourceCharacterSlug("ingrid");
        FrameData modern = new FrameData();
        modern.setId(2L);
        modern.setCharacterId(99L);
        modern.setControlType("modern");
        modern.setDisplayOrder(2);
        modern.setMoveName("手动现代轻拳");
        modern.setManualOverride(false);
        modern.setSourceCharacterSlug("ingrid");
        FrameData staleAutomatic = new FrameData();
        staleAutomatic.setId(3L);
        staleAutomatic.setCharacterId(99L);
        staleAutomatic.setControlType("classic");
        staleAutomatic.setDisplayOrder(2);
        staleAutomatic.setMoveName("官网已删除招式");
        staleAutomatic.setManualOverride(false);
        staleAutomatic.setSourceCharacterSlug("ingrid");
        FrameData staleManual = new FrameData();
        staleManual.setId(4L);
        staleManual.setCharacterId(99L);
        staleManual.setControlType("classic");
        staleManual.setDisplayOrder(3);
        staleManual.setMoveName("管理员保留招式");
        staleManual.setManualOverride(true);
        staleManual.setSourceCharacterSlug("ingrid");
        when(frameDataMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(classic, modern, staleAutomatic, staleManual));
        List<FrameData> updatedFrames = new ArrayList<>();
        doAnswer(invocation -> {
            updatedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).updateById(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        verify(frameDataMapper, never()).delete(any(Wrapper.class));
        verify(frameDataMapper, never()).insert(any(FrameData.class));
        verify(frameDataMapper).deleteById(3L);
        verify(frameDataMapper, never()).deleteById(4L);
        assertThat(updatedFrames).hasSize(2);
        assertThat(classic.getMoveName()).isEqualTo("手动中文轻拳");
        assertThat(classic.getStartup()).isEqualTo("4");
        assertThat(classic.getDisplayOrder()).isEqualTo(1);
        assertThat(modern.getMoveName()).isEqualTo("ML");
        assertThat(modern.getStartup()).isEqualTo("4");
        assertThat(modern.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void syncOneSelectsCurrentCharacterFromSharedFrameChunk() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ryu/frame";
        String chunkUrl = "https://www.streetfighter.com/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><span>Standing Light Punch</span><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" /></p></td><td>4</td><td>4-6</td><td>7</td><td>5</td><td>-1</td><td>-</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td></tr>
                                </tbody></table>
                                <script src="/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js"></script>
                                """;
                    }
                    if (url.equals(chunkUrl)) {
                        return """
                                let chars=JSON.parse('{"G":["ryu","ingrid"]}');
                                let ingrid=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P（ライトタッチ）","command":"LP","command_modern":"弱","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5"}]}');
                                let ryu=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P（ジャブ）","command":"MP","command_modern":"中","attribute":"上","damage":"500","startup_frame":"5","active_frame":"5-7","recovery_frame":"8","block_frame":"-2","hit_frame":"4"}]}');
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ryu", "RYU"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).hasSize(2);
        assertThat(insertedFrames.get(0).getMoveName()).isEqualTo("MP");
        assertThat(insertedFrames.get(1).getMoveName()).isEqualTo("MM");
    }

    @Test
    void syncOneSelectsCharacterWhenSharedChunkOmitsNewestFrameBlock() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        String chunkUrl = "https://www.streetfighter.com/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" /></p></td><td>4</td><td>4-6</td><td>7</td><td>5</td><td>-1</td><td>-</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td></tr>
                                </tbody></table>
                                <script src="/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js"></script>
                                """;
                    }
                    if (url.equals(chunkUrl)) {
                        return """
                                let chars=JSON.parse('{"G":["ryu","luke","ingrid","yasmine"]}');
                                let ingrid=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P（ライトタッチ）","command":"LP","command_modern":"弱","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5"}]}');
                                let luke=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P（ジャブ）","command":"MP","command_modern":"中","attribute":"上","damage":"500","startup_frame":"5","active_frame":"5-7","recovery_frame":"8","block_frame":"-2","hit_frame":"4"}]}');
                                let ryu=JSON.parse('{"frame":[{"webId":"100","skill":"立ち中P（ストレート）","command":"MP","command_modern":"中","attribute":"上","damage":"600","startup_frame":"6","active_frame":"6-8","recovery_frame":"10","block_frame":"-2","hit_frame":"4"}]}');
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).hasSize(2);
        assertThat(insertedFrames.get(0).getMoveName()).isEqualTo("LP");
        assertThat(insertedFrames.get(1).getMoveName()).isEqualTo("ML");
    }

    @Test
    void syncOneMatchesIngridWhenMissingFrameBlocksAreDistributedAcrossCharacterList() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        String chunkUrl = "https://www.streetfighter.com/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" /></p></td><td>4</td><td>4-6</td><td>7</td><td>5</td><td>-1</td><td>-</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td></tr>
                                </tbody></table>
                                <script src="/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js"></script>
                                """;
                    }
                    if (url.equals(chunkUrl)) {
                        return """
                                let chars=JSON.parse('{"G":["ryu","luke","alex","ingrid","yasmine"]}');
                                let ingrid=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P（ライトタッチ）","command":"LP","command_modern":"弱","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5"}]}');
                                let alex=JSON.parse('{"frame":[{"webId":"100","skill":"立ち強P","command":"HP","command_modern":"強","attribute":"上","damage":"900","startup_frame":"10","active_frame":"10-12","recovery_frame":"20","block_frame":"-5","hit_frame":"3"}]}');
                                let ryu=JSON.parse('{"frame":[{"webId":"100","skill":"立ち中K","command":"MK","command_modern":"中","attribute":"上","damage":"500","startup_frame":"7","active_frame":"7-9","recovery_frame":"15","block_frame":"-3","hit_frame":"2"}]}');
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).extracting(FrameData::getControlType)
                .containsExactly("classic", "modern");
        assertThat(insertedFrames).extracting(FrameData::getMoveName)
                .containsExactly("LP", "ML");
    }

    @Test
    void syncOneRejectsAWeakChunkMatchInsteadOfAssigningAnotherCharactersFrames() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/zangief/frame";
        String chunkUrl = "https://www.streetfighter.com/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js";
        String page = "<table><tbody>"
                + renderedRow("icon_punch_l.png", "4", "300")
                + renderedRow("icon_kick_l.png", "4", "300")
                + renderedRow("icon_punch_m.png", "8", "600")
                + renderedRow("key-circle.png", "5", "2500")
                + renderedRow("key-circle.png", "5", "2900")
                + renderedRow("key-circle.png", "6", "4800")
                + "</tbody></table><script src=\"/6/_next/static/chunks/pages/character/%5Bname%5D/frame-test.js\"></script>";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return page;
                    }
                    if (url.equals(chunkUrl)) {
                        return """
                                let chars=JSON.parse('{"G":["zangief","alex","ingrid","yasmine"]}');
                                let ingrid=JSON.parse('{"frame":[{"skill":"A","command":"LP","command_modern":"弱","startup_frame":"4","damage":"300"},{"skill":"B","command":"LK","command_modern":"弱","startup_frame":"4","damage":"300"},{"skill":"C","command":"MP","command_modern":"中","startup_frame":"8","damage":"600"},{"skill":"D","command":"236 + LP","command_modern":"SP","startup_frame":"12","damage":"800"},{"skill":"E","command":"214 + MP","command_modern":"2 + SP","startup_frame":"15","damage":"900"},{"skill":"F","command":"236236 + HP","command_modern":"SP + MH","startup_frame":"10","damage":"3000"}]}');
                                let alex=JSON.parse('{"frame":[{"skill":"A","command":"HP","command_modern":"強","startup_frame":"10","damage":"900"}]}');
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        service.syncOne(new OfficialFrameSyncService.OfficialCharacter("zangief", "ZANGIEF"));

        assertThat(insertedFrames).hasSize(6);
        assertThat(insertedFrames).allMatch(frame -> "classic".equals(frame.getControlType()));
        assertThat(insertedFrames).extracting(FrameData::getMoveName)
                .doesNotContain("236 + LP", "214 + MP", "236236 + HP");
    }

    @Test
    void syncOneKeepsJsonEscapesValidWhenFrameChunkContainsEscapedNewlines() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <script>let k=JSON.parse('{"frame":[{"webId":"100","skill":"立ち弱P","command":"LP","command_modern":"弱","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5","translation":"First line\\\\nSecond line"}]}')</script>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).hasSize(2);
        assertThat(insertedFrames.get(0).getMiscellaneous())
                .contains("First line")
                .contains("Second line");
        assertThat(insertedFrames.get(1).getMoveName()).isEqualTo("ML");
    }

    @Test
    void syncOneKeepsEscapedQuotesInsideOfficialJsonStringValues() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <script>let k=JSON.parse('{"frame":[{"webId":"100","skill":"SA","command":"236236 + LP","command_modern":"SP","attribute":"上","damage":"3000","startup_frame":"8","active_frame":"8-10","recovery_frame":"40","block_frame":"-30","hit_frame":"D","translation":"\\\\\\"With Classic Controls\\\\\\" deals damage"}]}')</script>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).hasSize(2);
        assertThat(insertedFrames.get(0).getMiscellaneous()).contains("With Classic Controls");
        assertThat(insertedFrames.get(1).getMoveName()).isEqualTo("SP");
    }

    @Test
    void syncOneNormalizesOfficialModernCommandText() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ingrid/frame";
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <script>let k=JSON.parse('{"frame":[{"webId":"100","skill":"SA","command":"236236 + 弱","command_modern":"（サンシンボルストック1つ）2 + AUTO + SP/214 + 攻撃二つ","attribute":"上","damage":"300","startup_frame":"4","active_frame":"4-6","recovery_frame":"7","block_frame":"-1","hit_frame":"5"},{"webId":"101","skill":"Throw","command":"LP+LK","command_modern":"（近距離で）N or 6 + 弱中","attribute":"投","damage":"1200","startup_frame":"5","active_frame":"5-7","recovery_frame":"20","block_frame":"","hit_frame":""},{"webId":"102","skill":"Hold","command":"HP","command_modern":"AUTO + 強ホールド","attribute":"上","damage":"900","startup_frame":"12","active_frame":"12-15","recovery_frame":"20","block_frame":"-5","hit_frame":"3"}]}')</script>
                                """;
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ingrid", "INGRID"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames)
                .extracting(FrameData::getMoveName)
                .contains("STOCK_1 2 + AUTO + SP / 214 + ATKATK", "NEAR 5 / 6 + MLMM", "AUTO + MH HOLD_OK");
    }

    @Test
    void syncOneParsesLargeOfficialChunkWithoutRegexStackOverflow() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        FrameDataMapper frameDataMapper = mock(FrameDataMapper.class);
        String expectedUrl = BASE_URL + "/ryu/frame";
        String chunkUrl = "https://www.streetfighter.com/6/_next/static/chunks/pages/character/%5Bname%5D/frame-large.js";
        String noisyText = "x\\'y\\\\z".repeat(20_000);
        OfficialFrameSyncService service = new OfficialFrameSyncService(
                characterMapper,
                frameDataMapper,
                BASE_URL,
                0,
                url -> {
                    if (url.equals(expectedUrl)) {
                        return """
                                <table><tbody>
                                  <tr><td class="frame_fixed_m__icTnd frame_skill__tLJuM"><p class="frame_classic___gpLR"><img src="/6/assets/images/common/controller/icon_punch_l.png" alt="" /></p></td><td>4</td><td>4-6</td><td>7</td><td>5</td><td>-1</td><td>-</td><td>300</td><td></td><td>250</td><td>-500</td><td>-2000</td><td>300</td><td>High</td><td></td></tr>
                                </tbody></table>
                                <script src="/6/_next/static/chunks/pages/character/%5Bname%5D/frame-large.js"></script>
                                """;
                    }
                    if (url.equals(chunkUrl)) {
                        return "let noise=JSON.parse('{\"ignored\":\"" + noisyText + "\"}');"
                                + "let chars=JSON.parse('{\"G\":[\"ryu\"]}');"
                                + "let ryu=JSON.parse('{\"frame\":[{\"webId\":\"100\",\"skill\":\"立ち弱P\",\"command\":\"LP\",\"command_modern\":\"弱\",\"attribute\":\"上\",\"damage\":\"300\",\"startup_frame\":\"4\",\"active_frame\":\"4-6\",\"recovery_frame\":\"7\",\"block_frame\":\"-1\",\"hit_frame\":\"5\"}]}');";
                    }
                    throw new IllegalArgumentException("Unexpected URL: " + url);
                });
        List<FrameData> insertedFrames = new ArrayList<>();
        when(characterMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            CharacterInfo character = invocation.getArgument(0);
            character.setId(99L);
            return 1;
        }).when(characterMapper).insert(any(CharacterInfo.class));
        doAnswer(invocation -> {
            insertedFrames.add(invocation.getArgument(0));
            return 1;
        }).when(frameDataMapper).insert(any(FrameData.class));

        OfficialFrameSyncService.FrameSyncResult result = service.syncOne(
                new OfficialFrameSyncService.OfficialCharacter("ryu", "RYU"));

        assertThat(result.success()).isTrue();
        assertThat(insertedFrames).extracting(FrameData::getMoveName).containsExactly("LP", "ML");
    }

    private static String renderedRow(String icon, String startup, String damage) {
        return "<tr><td class=\"frame_skill\"><p class=\"frame_classic\">"
                + "<img src=\"/6/assets/images/common/controller/" + icon + "\" />"
                + "</p></td><td>" + startup + "</td><td></td><td></td><td></td><td></td><td></td><td>"
                + damage + "</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>";
    }

}
