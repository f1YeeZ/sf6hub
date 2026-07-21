package com.example.hubdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.hubdemo.entity.CharacterInfo;
import com.example.hubdemo.entity.FrameData;
import com.example.hubdemo.mapper.CharacterMapper;
import com.example.hubdemo.mapper.FrameDataMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OfficialFrameSyncService {
    private static final String CAPCOM_FRAME_SUFFIX = "/frame";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<OfficialCharacter> OFFICIAL_CHARACTERS = List.of(
            new OfficialCharacter("ryu", "RYU"),
            new OfficialCharacter("luke", "LUKE"),
            new OfficialCharacter("jamie", "JAMIE"),
            new OfficialCharacter("chunli", "CHUN-LI"),
            new OfficialCharacter("guile", "GUILE"),
            new OfficialCharacter("kimberly", "KIMBERLY"),
            new OfficialCharacter("juri", "JURI"),
            new OfficialCharacter("ken", "KEN"),
            new OfficialCharacter("blanka", "BLANKA"),
            new OfficialCharacter("dhalsim", "DHALSIM"),
            new OfficialCharacter("ehonda", "E.HONDA"),
            new OfficialCharacter("deejay", "DEE JAY"),
            new OfficialCharacter("manon", "MANON"),
            new OfficialCharacter("marisa", "MARISA"),
            new OfficialCharacter("jp", "JP"),
            new OfficialCharacter("zangief", "ZANGIEF"),
            new OfficialCharacter("lily", "LILY"),
            new OfficialCharacter("cammy", "CAMMY"),
            new OfficialCharacter("rashid", "RASHID"),
            new OfficialCharacter("aki", "A.K.I."),
            new OfficialCharacter("ed", "ED"),
            new OfficialCharacter("gouki_akuma", "AKUMA"),
            new OfficialCharacter("vega_mbison", "M.BISON"),
            new OfficialCharacter("terry", "TERRY"),
            new OfficialCharacter("mai", "MAI"),
            new OfficialCharacter("elena", "ELENA"),
            new OfficialCharacter("sagat", "SAGAT"),
            new OfficialCharacter("cviper", "C.VIPER"),
            new OfficialCharacter("alex", "ALEX"),
            new OfficialCharacter("ingrid", "INGRID")
    );

    private static final Pattern ROW_PATTERN = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
    private static final Pattern MOVE_CELL_PATTERN = Pattern.compile("<td[^>]*class=\"[^\"]*frame_skill[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASSIC_INPUT_PATTERN = Pattern.compile("<p[^>]*class=\"[^\"]*frame_classic[^>]*>(.*?)</p>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern MODERN_INPUT_PATTERN = Pattern.compile("<p[^>]*class=\"[^\"]*frame_modern[^>]*>(.*?)</p>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern FRAME_CHUNK_PATTERN = Pattern.compile("<script[^>]+src=\"([^\"]*/_next/static/chunks/pages/character/(?:%5Bname%5D|\\[name\\])/frame-[^\"]+\\.js)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARACTER_LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*[\"'][^\"']*/character/([a-z0-9_-]+)(?:[/\"'?#])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_DESC_PATTERN = Pattern.compile("<desc>([^<]+)</desc>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Map<String, String> FALLBACK_CHARACTER_NAMES = OFFICIAL_CHARACTERS.stream()
            .collect(LinkedHashMap::new, (map, character) -> map.put(character.slug(), character.name()), LinkedHashMap::putAll);
    private static final Map<String, String> CAPCOM_CHARACTER_SLUGS = capcomCharacterSlugs();
    private static final Map<String, String> INTERNAL_CHARACTER_SLUGS = internalCharacterSlugs();

    private final CharacterMapper characterMapper;
    private final FrameDataMapper frameDataMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final long requestDelayMs;
    private final OfficialPageClient pageClient;

    @Autowired
    public OfficialFrameSyncService(CharacterMapper characterMapper,
                                    FrameDataMapper frameDataMapper,
                                    @Value("${app.official-frame-sync.base-url}") String baseUrl,
                                    @Value("${app.official-frame-sync.request-delay-ms}") long requestDelayMs) {
        this(characterMapper, frameDataMapper, baseUrl, requestDelayMs, null);
    }

    OfficialFrameSyncService(CharacterMapper characterMapper,
                             FrameDataMapper frameDataMapper,
                             String baseUrl,
                             long requestDelayMs,
                             OfficialPageClient pageClient) {
        this.characterMapper = characterMapper;
        this.frameDataMapper = frameDataMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.requestDelayMs = requestDelayMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.pageClient = pageClient == null ? this::fetchFromNetwork : pageClient;
    }

    public FrameSyncSummary syncAll() {
        List<FrameSyncResult> results = new ArrayList<>();
        List<OfficialCharacter> officialCharacters = discoverOfficialCharacters();
        for (int index = 0; index < officialCharacters.size(); index++) {
            OfficialCharacter character = officialCharacters.get(index);
            try {
                results.add(syncOne(character));
            } catch (Exception e) {
                results.add(new FrameSyncResult(character.slug(), character.name(), 0, false, e.getMessage()));
            }
            if (index < officialCharacters.size() - 1 && requestDelayMs > 0) {
                sleepQuietly(requestDelayMs);
            }
        }
        long success = results.stream().filter(FrameSyncResult::success).count();
        int imported = results.stream().mapToInt(FrameSyncResult::importedCount).sum();
        return new FrameSyncSummary(results.size(), success, imported, results);
    }

    public FrameSyncSummary syncNewCharacters() {
        List<FrameSyncResult> results = new ArrayList<>();
        List<OfficialCharacter> officialCharacters = discoverOfficialCharacters();
        List<OfficialCharacter> missingCharacters = officialCharacters.stream()
                .filter(character -> !characterExists(character))
                .toList();
        for (int index = 0; index < missingCharacters.size(); index++) {
            OfficialCharacter character = missingCharacters.get(index);
            try {
                results.add(syncOne(character));
            } catch (Exception e) {
                results.add(new FrameSyncResult(character.slug(), character.name(), 0, false, e.getMessage()));
            }
            if (index < missingCharacters.size() - 1 && requestDelayMs > 0) {
                sleepQuietly(requestDelayMs);
            }
        }
        long success = results.stream().filter(FrameSyncResult::success).count();
        int imported = results.stream().mapToInt(FrameSyncResult::importedCount).sum();
        return new FrameSyncSummary(results.size(), success, imported, results);
    }

    public FrameSyncResult syncOne(String slug) {
        OfficialCharacter character = discoverOfficialCharacters().stream()
                .filter(item -> item.slug().equals(slug))
                .findFirst()
                .orElseGet(() -> new OfficialCharacter(slug, FALLBACK_CHARACTER_NAMES.getOrDefault(slug, slugToName(slug))));
        return syncOne(character);
    }

    @Transactional
    public FrameSyncResult syncOne(OfficialCharacter officialCharacter) {
        String url = frameUrl(officialCharacter);
        String html = fetch(url);
        List<FrameData> frames = parseFrames(html, officialCharacter.slug(), url);
        if (frames.stream().noneMatch(frame -> "modern".equals(frame.getControlType()))) {
            List<FrameData> chunkFrames = parseFramesFromFrameChunk(
                    html, officialCharacter.slug(), url, frames);
            if (!chunkFrames.isEmpty()) {
                frames = chunkFrames;
            }
        }
        if (frames.isEmpty()) {
            throw new IllegalStateException("No frame rows parsed from " + url);
        }

        CharacterInfo character = upsertCharacter(officialCharacter, url);
        syncFrames(character, officialCharacter.slug(), frames);
        return new FrameSyncResult(officialCharacter.slug(), officialCharacter.name(), frames.size(), true, "");
    }

    private void syncFrames(CharacterInfo character, String sourceCharacterSlug, List<FrameData> frames) {
        List<FrameData> existingFrames = frameDataMapper.selectList(new LambdaQueryWrapper<FrameData>()
                .eq(FrameData::getCharacterId, character.getId())
                .eq(FrameData::getSourceCharacterSlug, sourceCharacterSlug));
        Map<String, FrameData> existingByOrder = new HashMap<>();
        Map<String, List<FrameData>> existingByControl = new HashMap<>();
        if (existingFrames != null) {
            for (FrameData existing : existingFrames) {
                existingByOrder.put(frameSyncKey(existing), existing);
                existingByControl.computeIfAbsent(normalizeFrameControlType(existing), ignored -> new ArrayList<>())
                        .add(existing);
            }
        }
        Map<String, FrameData> existingByControlOrder = new HashMap<>();
        Comparator<FrameData> existingOrder = Comparator
                .comparing((FrameData frame) -> frame.getDisplayOrder() == null ? Integer.MAX_VALUE : frame.getDisplayOrder())
                .thenComparing(frame -> frame.getId() == null ? Long.MAX_VALUE : frame.getId());
        existingByControl.forEach((controlType, controlFrames) -> {
            controlFrames.sort(existingOrder);
            for (int index = 0; index < controlFrames.size(); index++) {
                existingByControlOrder.put(controlType + ":" + (index + 1), controlFrames.get(index));
            }
        });
        Set<FrameData> matchedExisting = new HashSet<>();
        for (FrameData incoming : frames) {
            incoming.setCharacterId(character.getId());
            FrameData existing = existingByOrder.get(frameSyncKey(incoming));
            if (existing != null && matchedExisting.contains(existing)) {
                existing = null;
            }
            if (existing == null) {
                existing = existingByControlOrder.get(frameSyncKey(incoming));
                if (existing != null && matchedExisting.contains(existing)) {
                    existing = null;
                }
            }
            if (existing == null) {
                frameDataMapper.insert(incoming);
                continue;
            }
            matchedExisting.add(existing);
            copyFrameValues(incoming, existing);
            frameDataMapper.updateById(existing);
        }
        if (existingFrames != null) {
            for (FrameData existing : existingFrames) {
                if (!matchedExisting.contains(existing)
                        && !Boolean.TRUE.equals(existing.getManualOverride())
                        && existing.getId() != null) {
                    frameDataMapper.deleteById(existing.getId());
                }
            }
        }
    }

    private static String frameSyncKey(FrameData frame) {
        String controlType = normalizeFrameControlType(frame);
        Integer displayOrder = frame.getDisplayOrder() == null ? 0 : frame.getDisplayOrder();
        return controlType + ":" + displayOrder;
    }

    private static String normalizeFrameControlType(FrameData frame) {
        return frame.getControlType() == null || frame.getControlType().isBlank()
                ? "classic"
                : frame.getControlType();
    }

    private static void copyFrameValues(FrameData source, FrameData target) {
        if (!Boolean.TRUE.equals(target.getManualOverride())) {
            target.setMoveName(source.getMoveName());
        }
        target.setStartup(source.getStartup());
        target.setActive(source.getActive());
        target.setRecovery(source.getRecovery());
        target.setOnBlock(source.getOnBlock());
        target.setOnHit(source.getOnHit());
        target.setCancel(source.getCancel());
        target.setDamage(source.getDamage());
        target.setComboScaling(source.getComboScaling());
        target.setDriveGainOnHit(source.getDriveGainOnHit());
        target.setDriveLossOnBlock(source.getDriveLossOnBlock());
        target.setDriveLossOnPunishCounter(source.getDriveLossOnPunishCounter());
        target.setSuperArtGain(source.getSuperArtGain());
        target.setProperties(source.getProperties());
        target.setMiscellaneous(source.getMiscellaneous());
        target.setSourceUrl(source.getSourceUrl());
        target.setSourceLang(source.getSourceLang());
        target.setSourceSyncedAt(source.getSourceSyncedAt());
        target.setDisplayOrder(source.getDisplayOrder());
    }

    private boolean characterExists(OfficialCharacter officialCharacter) {
        CharacterInfo character = characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                .eq(CharacterInfo::getOfficialSlug, officialCharacter.slug())
                .last("limit 1"));
        if (character != null) {
            return true;
        }
        return characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                .eq(CharacterInfo::getName, officialCharacter.name())
                .last("limit 1")) != null;
    }

    private CharacterInfo upsertCharacter(OfficialCharacter officialCharacter, String url) {
        CharacterInfo character = characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                .eq(CharacterInfo::getOfficialSlug, officialCharacter.slug())
                .last("limit 1"));
        if (character == null) {
            character = characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                    .eq(CharacterInfo::getName, officialCharacter.name())
                    .last("limit 1"));
        }
        if (character == null) {
            character = new CharacterInfo();
            character.setAvatar("");
            character.setDescription("Street Fighter 6 frame data imported from Capcom.");
            character.setArchetype("");
            character.setName(officialCharacter.name());
            character.setOfficialSlug(officialCharacter.slug());
            character.setOfficialFrameUrl(url);
            character.setDisplayOrder(nextCharacterDisplayOrder());
            characterMapper.insert(character);
            return character;
        }
        character.setName(officialCharacter.name());
        character.setOfficialSlug(officialCharacter.slug());
        character.setOfficialFrameUrl(url);
        characterMapper.updateById(character);
        return character;
    }

    private int nextCharacterDisplayOrder() {
        CharacterInfo last = characterMapper.selectOne(new LambdaQueryWrapper<CharacterInfo>()
                .orderByDesc(CharacterInfo::getDisplayOrder)
                .orderByDesc(CharacterInfo::getId)
                .last("limit 1"));
        int maxOrder = last == null || last.getDisplayOrder() == null ? 0 : last.getDisplayOrder();
        return maxOrder + 1;
    }

    private String fetch(String url) {
        return pageClient.fetch(url);
    }

    private String fetchFromNetwork(String url) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept", url.endsWith(".js") ? "application/javascript,*/*;q=0.8" : "application/json,text/html,*/*")
                    .GET();
            if (url.endsWith(".js")) {
                builder.header("Referer", baseUrl + "/ingrid/frame");
            }
            HttpRequest request = builder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 403 && isWindows()) {
                    return fetchWithPowerShell(url);
                }
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching " + url, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch " + url + ": " + e.getMessage(), e);
        }
    }

    private String fetchWithPowerShell(String url) {
        if (!isWindows()) {
            throw new IllegalStateException("HTTP 403 from " + url + "; PowerShell fallback is only available on Windows");
        }
        String command = """
                [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false);
                $headers = @{
                  'User-Agent' = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36';
                  'Accept' = 'application/javascript,application/json,text/html,*/*';
                  'Accept-Language' = 'zh-CN,zh;q=0.9,en;q=0.8'
                  'Referer' = 'https://www.streetfighter.com/6/zh-hans/character/ingrid/frame'
                };
                (Invoke-WebRequest -Uri $env:CAPCOM_FRAME_URL -UseBasicParsing -TimeoutSec 60 -Headers $headers).Content
                """;
        ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", command);
        builder.environment().put("CAPCOM_FRAME_URL", url);
        try {
            Process process = builder.start();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread outThread = streamTo(process.getInputStream(), stdout);
            Thread errThread = streamTo(process.getErrorStream(), stderr);
            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("PowerShell fetch timed out for " + url);
            }
            outThread.join(5000);
            errThread.join(5000);
            String body = stdout.toString(StandardCharsets.UTF_8);
            String error = stderr.toString(StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("PowerShell fetch failed for " + url + ": " + error);
            }
            if (body.isBlank()) {
                throw new IllegalStateException("PowerShell fetch returned empty body for " + url + (error.isBlank() ? "" : ": " + error));
            }
            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching " + url + " via PowerShell", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch " + url + " via PowerShell: " + e.getMessage(), e);
        }
    }

    private static Thread streamTo(InputStream input, ByteArrayOutputStream output) {
        Thread thread = new Thread(() -> {
            try (input; output) {
                input.transferTo(output);
            } catch (Exception ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private List<OfficialCharacter> discoverOfficialCharacters() {
        try {
            List<String> discovered = parseOfficialCharacterSlugs(fetch(baseUrl));
            if (discovered.isEmpty()) {
                return OFFICIAL_CHARACTERS;
            }
            Map<String, OfficialCharacter> characters = new LinkedHashMap<>();
            OFFICIAL_CHARACTERS.forEach(character -> characters.put(character.slug(), character));
            for (String capcomSlug : discovered) {
                String slug = internalSlug(capcomSlug);
                if (characters.containsKey(slug) || !hasCapcomFrameTable(slug)) {
                    continue;
                }
                characters.putIfAbsent(slug, new OfficialCharacter(slug,
                        FALLBACK_CHARACTER_NAMES.getOrDefault(slug, resolveOfficialName(slug))));
            }
            return new ArrayList<>(characters.values());
        } catch (Exception ignored) {
            return OFFICIAL_CHARACTERS;
        }
    }

    private boolean hasCapcomFrameTable(String slug) {
        try {
            String html = fetch(frameUrl(new OfficialCharacter(slug, slugToName(slug))));
            return MOVE_CELL_PATTERN.matcher(html).find();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveOfficialName(String slug) {
        try {
            String name = parseOfficialName(fetch(characterProfileUrl(slug)));
            if (!name.isBlank()) {
                return name;
            }
        } catch (Exception ignored) {
        }
        return slugToName(slug);
    }

    static List<String> parseOfficialCharacterSlugs(String html) {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        Matcher matcher = CHARACTER_LINK_PATTERN.matcher(html);
        while (matcher.find()) {
            String slug = matcher.group(1).toLowerCase();
            if (!slug.isBlank()) {
                seen.putIfAbsent(slug, Boolean.TRUE);
            }
        }
        return new ArrayList<>(seen.keySet());
    }

    static String parseOfficialName(String html) {
        Matcher descMatcher = SVG_DESC_PATTERN.matcher(html);
        String best = "";
        while (descMatcher.find()) {
            String value = cleanStatic(descMatcher.group(1));
            if (value.matches("[A-Z0-9 .'-]+") && !value.startsWith("YEAR") && value.length() <= 40) {
                best = value;
            }
        }
        if (!best.isBlank()) {
            return best;
        }

        Matcher titleMatcher = TITLE_PATTERN.matcher(html);
        if (titleMatcher.find()) {
            String title = cleanStatic(titleMatcher.group(1));
            int separator = title.indexOf('|');
            return separator > 0 ? title.substring(0, separator).trim() : title;
        }
        return "";
    }

    private List<FrameData> parseFrames(String html, String slug, String sourceUrl) {
        List<FrameData> framesFromJson = parseFramesFromOfficialJson(html, slug, sourceUrl);
        if (!framesFromJson.isEmpty()) {
            return framesFromJson;
        }
        return parseFramesFromRenderedTable(html, slug, sourceUrl);
    }

    private List<FrameData> parseFramesFromFrameChunk(String html, String slug, String sourceUrl,
                                                       List<FrameData> renderedFrames) {
        String chunkUrl = frameChunkUrl(html);
        if (chunkUrl.isBlank()) {
            return List.of();
        }
        try {
            return parseFramesFromOfficialJson(fetch(chunkUrl), slug, sourceUrl, renderedFrames);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String frameChunkUrl(String html) {
        Matcher matcher = FRAME_CHUNK_PATTERN.matcher(html == null ? "" : html);
        if (!matcher.find()) {
            return "";
        }
        String src = matcher.group(1).replace("\\/", "/");
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return src;
        }
        return "https://www.streetfighter.com" + (src.startsWith("/") ? src : "/" + src);
    }

    private List<FrameData> parseFramesFromOfficialJson(String html, String slug, String sourceUrl) {
        return parseFramesFromOfficialJson(html, slug, sourceUrl, List.of());
    }

    private List<FrameData> parseFramesFromOfficialJson(String html, String slug, String sourceUrl,
                                                         List<FrameData> renderedFrames) {
        List<FrameData> frames = new ArrayList<>();
        JsonNode rows = officialFrameRows(html, slug, renderedFrames);
        if (!rows.isArray()) {
            return frames;
        }
        int order = 1;
        for (JsonNode row : rows) {
            if (row.path("skill").asText("").isBlank()) {
                continue;
            }
            String classicMove = capcomCommandInput(row.path("command").asText(""), "classic");
            if (!classicMove.isBlank()) {
                frames.add(frameFromOfficialRow(row, classicMove, "classic", slug, sourceUrl, order));
            }
            String modernMove = capcomCommandInput(row.path("command_modern").asText(""), "modern");
            if (!modernMove.isBlank()) {
                frames.add(frameFromOfficialRow(row, modernMove, "modern", slug, sourceUrl, order));
            }
            if (!classicMove.isBlank() || !modernMove.isBlank()) {
                order++;
            }
        }
        return frames;
    }

    private static JsonNode officialFrameRows(String html, String slug, List<FrameData> renderedFrames) {
        List<String> characterSlugs = List.of();
        List<JsonNode> frameRows = new ArrayList<>();
        for (String jsString : jsonParseStrings(html)) {
            String json = unescapeJsString(jsString);
            try {
                JsonNode root = JSON.readTree(json);
                JsonNode characterList = root.path("G");
                if (characterList.isArray() && !characterList.isEmpty()) {
                    characterSlugs = new ArrayList<>();
                    for (JsonNode item : characterList) {
                        characterSlugs.add(internalSlug(item.asText("")));
                    }
                }
                JsonNode rows = root.path("frame");
                if (rows.isArray() && !rows.isEmpty()) {
                    frameRows.add(rows);
                }
            } catch (Exception ignored) {
            }
        }
        if (frameRows.size() == 1 && (renderedFrames == null || renderedFrames.isEmpty())) {
            return frameRows.get(0);
        }
        if (!characterSlugs.isEmpty() && characterSlugs.size() == frameRows.size()) {
            List<String> reversed = new ArrayList<>(characterSlugs);
            java.util.Collections.reverse(reversed);
            int index = reversed.indexOf(slug);
            if (index >= 0 && index < frameRows.size()) {
                return frameRows.get(index);
            }
        }
        if (!characterSlugs.isEmpty() && characterSlugs.size() == frameRows.size() + 1) {
            List<String> reversed = new ArrayList<>(characterSlugs);
            java.util.Collections.reverse(reversed);
            if (!reversed.isEmpty() && "yasmine".equals(reversed.get(0))) {
                int index = reversed.indexOf(slug) - 1;
                if (index >= 0 && index < frameRows.size()) {
                    return frameRows.get(index);
                }
            }
        }
        JsonNode renderedMatch = matchRenderedFrameRows(frameRows, renderedFrames);
        if (renderedMatch != null) {
            return renderedMatch;
        }
        if (renderedFrames != null && !renderedFrames.isEmpty()) {
            return JSON.createArrayNode();
        }
        if (!characterSlugs.isEmpty() && characterSlugs.size() > frameRows.size()) {
            List<String> reversed = new ArrayList<>(characterSlugs);
            java.util.Collections.reverse(reversed);
            int index = reversed.indexOf(slug);
            int missingCount = reversed.size() - frameRows.size();
            int shiftedIndex = index - missingCount;
            if (shiftedIndex >= 0 && shiftedIndex < frameRows.size()) {
                return frameRows.get(shiftedIndex);
            }
        }
        return JSON.createArrayNode();
    }

    private static JsonNode matchRenderedFrameRows(List<JsonNode> frameRows, List<FrameData> renderedFrames) {
        if (renderedFrames == null || renderedFrames.isEmpty() || frameRows.isEmpty()) {
            return null;
        }
        List<FrameData> classicFrames = renderedFrames.stream()
                .filter(frame -> "classic".equals(normalizeFrameControlType(frame)))
                .toList();
        if (classicFrames.isEmpty()) {
            return null;
        }

        JsonNode bestRows = null;
        int bestScore = Integer.MIN_VALUE;
        int runnerUpScore = Integer.MIN_VALUE;
        int bestMoveMatches = 0;
        int bestCompared = 0;
        int bestLengthDifference = Integer.MAX_VALUE;
        for (JsonNode rows : frameRows) {
            List<JsonNode> classicRows = new ArrayList<>();
            for (JsonNode row : rows) {
                if (!row.path("skill").asText("").isBlank()
                        && !row.path("command").asText("").isBlank()) {
                    classicRows.add(row);
                }
            }
            int score = -Math.abs(classicFrames.size() - classicRows.size()) * 6;
            int compared = Math.min(classicFrames.size(), classicRows.size());
            int moveMatches = 0;
            for (int index = 0; index < compared; index++) {
                FrameData rendered = classicFrames.get(index);
                JsonNode candidate = classicRows.get(index);
                String candidateMove = capcomCommandInput(candidate.path("command").asText(""), "classic");
                if (rendered.getMoveName() != null && rendered.getMoveName().equals(candidateMove)) {
                    score += 4;
                    moveMatches++;
                }
                score += matchingFrameFieldScore(rendered.getStartup(), candidate.path("startup_frame").asText(""));
                score += matchingFrameFieldScore(rendered.getDamage(), candidate.path("damage").asText(""));
            }
            if (score > bestScore) {
                runnerUpScore = bestScore;
                bestScore = score;
                bestRows = rows;
                bestMoveMatches = moveMatches;
                bestCompared = compared;
                bestLengthDifference = Math.abs(classicFrames.size() - classicRows.size());
            } else if (score > runnerUpScore) {
                runnerUpScore = score;
            }
        }

        int minimumMoveMatches = bestCompared < 4
                ? bestCompared
                : bestCompared / 2 + 1;
        int maximumLengthDifference = Math.max(2, (int) Math.ceil(classicFrames.size() * 0.1));
        int minimumScoreLead = Math.max(6, bestCompared * 3 / 2);
        return bestCompared > 0
                && bestMoveMatches >= minimumMoveMatches
                && bestLengthDifference <= maximumLengthDifference
                && bestScore - runnerUpScore >= minimumScoreLead
                ? bestRows
                : null;
    }

    private static int matchingFrameFieldScore(String renderedValue, String candidateValue) {
        if (renderedValue == null || renderedValue.isBlank() || candidateValue == null || candidateValue.isBlank()) {
            return 0;
        }
        return renderedValue.trim().equals(candidateValue.trim()) ? 1 : 0;
    }

    private static List<String> jsonParseStrings(String html) {
        if (html == null || html.isEmpty()) {
            return List.of();
        }
        String needle = "JSON.parse('";
        List<String> values = new ArrayList<>();
        int searchFrom = 0;
        while (searchFrom < html.length()) {
            int start = html.indexOf(needle, searchFrom);
            if (start < 0) {
                break;
            }
            int valueStart = start + needle.length();
            int valueEnd = findJsSingleQuotedStringEnd(html, valueStart);
            if (valueEnd < 0) {
                break;
            }
            values.add(html.substring(valueStart, valueEnd));
            searchFrom = valueEnd + 1;
        }
        return values;
    }

    private static int findJsSingleQuotedStringEnd(String value, int start) {
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '\'') {
                return index;
            }
        }
        return -1;
    }

    static String unescapeJsString(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\' && index + 1 < value.length()) {
                char next = value.charAt(index + 1);
                index++;
                switch (next) {
                    case '\'' -> builder.append('\'');
                    case '\\' -> builder.append('\\');
                    case '"' -> builder.append('"');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'x' -> {
                        if (index + 2 < value.length()) {
                            String hex = value.substring(index + 1, index + 3);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 2;
                        } else {
                            builder.append('\\').append(next);
                        }
                    }
                    case 'u' -> {
                        if (index + 4 < value.length()) {
                            String hex = value.substring(index + 1, index + 5);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        } else {
                            builder.append('\\').append(next);
                        }
                    }
                    default -> builder.append(next);
                }
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static FrameData frameFromOfficialRow(JsonNode row, String moveName, String controlType,
                                                  String slug, String sourceUrl, int displayOrder) {
        FrameData frame = new FrameData();
        frame.setControlType(controlType);
        frame.setMoveName(moveName);
        frame.setStartup(text(row, "startup_frame"));
        frame.setActive(text(row, "active_frame"));
        frame.setRecovery(recoveryText(row));
        frame.setOnHit(text(row, "hit_frame"));
        frame.setOnBlock(text(row, "block_frame"));
        frame.setCancel(text(row, "web_cancel"));
        frame.setDamage(text(row, "damage"));
        frame.setComboScaling(arrayText(row.path("combo_correct")));
        frame.setDriveGainOnHit(text(row, "drive_gauge_gain_hit"));
        frame.setDriveLossOnBlock(text(row, "drive_gauge_lose_dguard"));
        frame.setDriveLossOnPunishCounter(text(row, "drive_gauge_lose_punish"));
        frame.setSuperArtGain(text(row, "sa_gauge_gain"));
        frame.setProperties(capcomProperty(text(row, "attribute")));
        frame.setMiscellaneous(miscText(row));
        frame.setSourceUrl(sourceUrl);
        frame.setSourceCharacterSlug(slug);
        frame.setSourceLang("capcom");
        frame.setSourceSyncedAt(LocalDateTime.now());
        frame.setDisplayOrder(displayOrder);
        frame.setManualOverride(false);
        return frame;
    }

    private static String text(JsonNode row, String field) {
        JsonNode node = row.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return "";
        }
        return cleanStatic(node.asText(""));
    }

    private static String arrayText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (!node.isArray()) {
            return cleanStatic(node.asText(""));
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = cleanStatic(item.asText(""));
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return String.join("; ", values);
    }

    private static String recoveryText(JsonNode row) {
        String recovery = text(row, "recovery_frame");
        if (!recovery.isBlank()) {
            return recovery;
        }
        String total = text(row, "frame");
        return total.isBlank() ? "" : "Total: " + total;
    }

    private static String miscText(JsonNode row) {
        List<String> values = new ArrayList<>();
        String translation = text(row, "translation").replaceAll("\\R+", "; ");
        if (!translation.isBlank()) {
            values.add(translation);
        }
        String note = arrayText(row.path("note"));
        if (!note.isBlank()) {
            values.add(note);
        }
        return String.join("; ", values);
    }

    private List<FrameData> parseFramesFromRenderedTable(String html, String slug, String sourceUrl) {
        List<FrameData> frames = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(html == null ? "" : html);
        int order = 1;
        while (rowMatcher.find()) {
            String rowHtml = rowMatcher.group(1);
            Matcher moveCellMatcher = MOVE_CELL_PATTERN.matcher(rowHtml);
            if (!moveCellMatcher.find()) {
                continue;
            }
            String moveCellHtml = moveCellMatcher.group(1);
            List<String> cells = cells(rowHtml);
            if (cells.size() < 15) {
                continue;
            }
            int previousSize = frames.size();
            addRenderedFrame(frames, cells, capcomInput(moveCellHtml, CLASSIC_INPUT_PATTERN),
                    "classic", slug, sourceUrl, order);
            addRenderedFrame(frames, cells, capcomInput(moveCellHtml, MODERN_INPUT_PATTERN),
                    "modern", slug, sourceUrl, order);
            if (frames.size() > previousSize) {
                order++;
            }
        }
        return frames;
    }

    private static void addRenderedFrame(List<FrameData> frames, List<String> cells, String moveName,
                                         String controlType, String slug, String sourceUrl, int displayOrder) {
        if (moveName.isBlank()) {
            return;
        }
        FrameData frame = new FrameData();
        frame.setControlType(controlType);
        frame.setMoveName(moveName);
        frame.setStartup(cellAt(cells, 1));
        frame.setActive(cellAt(cells, 2));
        frame.setRecovery(cellAt(cells, 3));
        frame.setOnHit(cellAt(cells, 4));
        frame.setOnBlock(cellAt(cells, 5));
        frame.setCancel(cellAt(cells, 6));
        frame.setDamage(cellAt(cells, 7));
        frame.setComboScaling(cellAt(cells, 8));
        frame.setDriveGainOnHit(cellAt(cells, 9));
        frame.setDriveLossOnBlock(cellAt(cells, 10));
        frame.setDriveLossOnPunishCounter(cellAt(cells, 11));
        frame.setSuperArtGain(cellAt(cells, 12));
        frame.setProperties(capcomProperty(cellAt(cells, 13)));
        frame.setMiscellaneous(cellAt(cells, 14));
        frame.setSourceUrl(sourceUrl);
        frame.setSourceCharacterSlug(slug);
        frame.setSourceLang("capcom");
        frame.setSourceSyncedAt(LocalDateTime.now());
        frame.setDisplayOrder(displayOrder);
        frame.setManualOverride(false);
        frames.add(frame);
    }

    private static String cellAt(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return "-".equals(value) ? "" : value;
    }

    private List<String> cells(String rowHtml) {
        List<String> cells = new ArrayList<>();
        Matcher cellMatcher = CELL_PATTERN.matcher(rowHtml);
        while (cellMatcher.find()) {
            cells.add(cleanStatic(cellMatcher.group(1)));
        }
        return cells;
    }

    private static String capcomClassicInput(String moveCellHtml) {
        return capcomInput(moveCellHtml, CLASSIC_INPUT_PATTERN);
    }

    private static String capcomInput(String moveCellHtml, Pattern inputPattern) {
        Matcher inputMatcher = inputPattern.matcher(moveCellHtml);
        if (!inputMatcher.find()) {
            return "";
        }
        String inputHtml = inputMatcher.group(1);
        boolean jumping = cleanStatic(inputHtml).toLowerCase(Locale.ROOT).contains("during a jump");
        List<String> tokens = new ArrayList<>();
        if (jumping) {
            tokens.add("J");
        }
        Matcher imageMatcher = IMG_SRC_PATTERN.matcher(inputHtml);
        while (imageMatcher.find()) {
            String token = capcomInputToken(imageMatcher.group(1));
            if (token.isBlank()) {
                continue;
            }
            if (">".equals(token) || "/".equals(token)) {
                if (!tokens.isEmpty() && !tokens.get(tokens.size() - 1).equals(token)) {
                    tokens.add(token);
                }
                continue;
            }
            tokens.add(token);
        }
        return joinCapcomInputTokens(tokens);
    }

    static String capcomCommandInput(String command, String controlType) {
        if (command == null || command.isBlank()) {
            return "";
        }
        boolean modern = "modern".equals(controlType);
        String value = normalizeCommandConditions(command)
                .replace("\\n", " ")
                .replace("LPMPHP", "PP")
                .replace("LKMKHK", "KK")
                .replaceAll("4\\s*溜\\s*め\\s*(?:[+＋]\\s*)?6", "CHARGE4 + 6")
                .replaceAll("2\\s*溜\\s*め\\s*(?:[+＋]\\s*)?8", "CHARGE2 + 8")
                .replaceAll("4\\s*溜\\s*め", "CHARGE4")
                .replaceAll("2\\s*溜\\s*め", "CHARGE2")
                .replaceAll("6\\s*溜\\s*め", "CHARGE6")
                .replaceAll("23626\\s*[|｜]\\s*26236", "236236")
                .replaceAll("2142[456]\\s*[|｜]\\s*24214", "214214")
                .replace("5252", "22")
                .replaceAll("236\\s*[|｜]\\s*239", "236")
                .replace("626", "623")
                .replaceAll("2\\s*[|｜]\\s*6\\s*[|｜]\\s*3", "3")
                .replaceAll("2\\s*[|｜]\\s*4\\s*[|｜]\\s*1", "1")
                .replaceAll("6314\\s*[|｜]\\s*6214\\s*[|｜]\\s*6324", "63214")
                .replace("（ジャンプ中に）", "J")
                .replace("（垂直ジャンプ中に）", "J")
                .replace("（垂直or前ジャンプ中に）", "J")
                .replace("(During a jump)", "J")
                .replace("（近距離で）", "NEAR ")
                .replace("（背面から近距離で）", "NEAR ")
                .replace("（体力25%以下で）", "LOW_LIFE ")
                .replace("（ガード中 or ドライブパリィ成立中に）", "GUARD_OR_PARRY ")
                .replace("（起き上がり時に）", "ON_WAKEUP ")
                .replace("（ドライブパリィ中に）", "PARRY ")
                .replaceAll("（[^）]*後に）", " FOLLOWUP ")
                .replaceAll("（[^）]*攻撃前に）", " BEFORE_ATTACK ")
                .replace("（入力なし）", " NO_INPUT ")
                .replace("ガード方向", "GUARD_DIR")
                .replaceAll("（[^）]*(?:ストック無し|ストック0)[^）]*）", " STOCK_0 ")
                .replaceAll("（[^）]*ストック1つ[^）]*）", " STOCK_1 ")
                .replaceAll("（[^）]*ストック2つ以上[^）]*）", " STOCK_2 ")
                .replaceAll("（[^）]*(?:中に|状態の相手に対して|スタンス|ステップイン)[^）]*）", " ")
                .replace("ホールド", " HOLD_OK ")
                .replace("一回転", "360")
                .replace("1回転", "360")
                .replace("二回転", "720")
                .replace("2回転", "720")
                .replace("360360", "720")
                .replace("＋", "+")
                .replace("＞", ">")
                .replace("｜", "/")
                .replace("|", "/")
                .replaceAll("(?i)\\s+or\\s+", " / ")
                .replaceAll("(?i)(?<=[A-Z0-9])\\s*or\\s*(?=[A-Z0-9])", " / ")
                .replaceAll("(?<![A-Za-z])N(?![A-Za-z])", "5")
                .replaceAll("\\s+", " ");
        String light = modern ? "ML" : "LP";
        String medium = modern ? "MM" : "MP";
        String heavy = modern ? "MH" : "HP";
        value = value
                .replace("攻撃三つ", "ATKATKATK")
                .replace("攻撃二つ", "ATKATK")
                .replace("弱中", light + medium)
                .replace("中強", medium + heavy)
                .replace("弱強", light + heavy)
                .replace("強SP", heavy + "SP")
                .replace("SP強", "SP" + heavy)
                .replace("弱", light)
                .replace("中", medium)
                .replace("強", heavy)
                .replace("攻撃", "ATK")
                .replace("投", "THROW")
                .replaceAll("(?<![A-Z0-9])2\\s+HOLD_OK", "CHARGE2")
                .replaceAll("(?<![A-Z0-9])4\\s+HOLD_OK", "CHARGE4")
                .replaceAll("(?<![A-Z0-9])6\\s+HOLD_OK", "CHARGE6")
                .replaceAll("\\s*\\+\\s*", " + ")
                .replaceAll("\\s*/\\s*", " / ")
                .replaceAll("\\s*>\\s*", " > ");
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeCommandConditions(String command) {
        if (command.startsWith("※") && command.contains("打撃を受ける")) {
            return "ON_STRIKE";
        }
        StringBuilder normalized = new StringBuilder(command.length());
        for (int index = 0; index < command.length(); index++) {
            char current = command.charAt(index);
            if (current != '（' && current != '(') {
                normalized.append(current);
                continue;
            }
            char opening = current;
            char closing = current == '（' ? '）' : ')';
            int depth = 1;
            int end = index + 1;
            while (end < command.length() && depth > 0) {
                char candidate = command.charAt(end);
                if (candidate == opening) {
                    depth++;
                } else if (candidate == closing) {
                    depth--;
                }
                end++;
            }
            if (depth != 0) {
                normalized.append(current);
                continue;
            }
            String condition = command.substring(index + 1, end - 1);
            normalized.append(' ').append(commandConditionToken(condition)).append(' ');
            index = end - 1;
        }
        return normalized.toString();
    }

    private static String commandConditionToken(String condition) {
        String compact = condition.replace(" ", "");
        if (compact.contains("ジャンプ")) {
            return "J";
        }
        if (compact.toLowerCase(Locale.ROOT).contains("duringajump")) {
            return "J";
        }
        if (compact.contains("近距離")) {
            return "NEAR";
        }
        if (compact.contains("体力25%以下")) {
            return "LOW_LIFE";
        }
        if (compact.contains("ガード中") && compact.contains("ドライブパリィ")) {
            return "GUARD_OR_PARRY";
        }
        if (compact.contains("起き上がり")) {
            return "ON_WAKEUP";
        }
        if (compact.contains("ドライブパリィ")) {
            return "PARRY";
        }
        if (compact.contains("ストック無し") || compact.contains("ストック0")) {
            return "STOCK_0";
        }
        if (compact.contains("ストック1つ") || compact.contains("ストックが1以上")) {
            return "STOCK_1";
        }
        if (compact.contains("ストック2つ") || compact.contains("ストックが2以上")) {
            return "STOCK_2";
        }
        if (compact.contains("酔いLv1")) {
            return "DRUNK_1";
        }
        if (compact.contains("酔いLv2")) {
            return "DRUNK_2";
        }
        if (compact.contains("酔いLv3")) {
            return "DRUNK_3";
        }
        if (compact.contains("酔いLv4")) {
            return "DRUNK_4";
        }
        if (compact.contains("後に")) {
            return "FOLLOWUP";
        }
        if (compact.contains("攻撃前に")) {
            return "BEFORE_ATTACK";
        }
        if (compact.contains("入力なし")) {
            return "NO_INPUT";
        }
        if (compact.contains("遠距離")) {
            return "FAR";
        }
        return "DURING_MOVE";
    }

    private static String joinCapcomInputTokens(List<String> tokens) {
        StringBuilder builder = new StringBuilder();
        String previous = "";
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (">".equals(token) || "/".equals(token) || "+".equals(token)) {
                if (builder.length() > 0 && !isInputSeparator(previous)) {
                    builder.append(' ').append(token).append(' ');
                }
            } else {
                builder.append(token);
            }
            previous = token;
        }
        return builder.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean isInputSeparator(String token) {
        return ">".equals(token) || "/".equals(token) || "+".equals(token);
    }

    private static String capcomInputToken(String src) {
        String filename = src == null ? "" : src.substring(src.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return switch (filename) {
            case "icon_punch_l.png" -> "LP";
            case "icon_punch_m.png" -> "MP";
            case "icon_punch_h.png" -> "HP";
            case "icon_kick_l.png" -> "LK";
            case "icon_kick_m.png" -> "MK";
            case "icon_kick_h.png" -> "HK";
            case "icon_punch.png" -> "P";
            case "icon_kick.png" -> "K";
            case "key-d.png" -> "2";
            case "key-dl.png" -> "1";
            case "key-dr.png" -> "3";
            case "key-l.png" -> "4";
            case "key-r.png" -> "6";
            case "key-u.png" -> "8";
            case "key-ul.png" -> "7";
            case "key-ur.png" -> "9";
            case "key-nutral.png" -> "5";
            case "key-circle.png" -> "360";
            case "key-all.png" -> "ATK";
            case "key-barrage.png" -> "HD";
            case "key-dc.png" -> "CHARGE2";
            case "key-lc.png" -> "CHARGE4";
            case "key-rc.png" -> "CHARGE6";
            case "key-or.png" -> "/";
            case "arrow_3.png" -> ">";
            case "key-plus.png" -> "+";
            case "modern_l.png" -> "ML";
            case "modern_m.png" -> "MM";
            case "modern_h.png" -> "MH";
            case "modern_sp.png" -> "SP";
            case "modern_dl.png" -> "DI";
            case "modern_dp.png" -> "DP";
            case "modern_auto.png" -> "AUTO";
            case "icon_throw.png" -> "THROW";
            default -> "";
        };
    }

    private static String capcomProperty(String value) {
        return switch (value) {
            case "High" -> "H";
            case "Low" -> "L";
            case "Mid" -> "M";
            case "Throw" -> "T";
            case "Projectile" -> "P";
            case "上" -> "H";
            case "下" -> "L";
            case "中" -> "M";
            case "投" -> "T";
            case "飛" -> "P";
            case "空中" -> "MP";
            default -> value;
        };
    }

    private static String cleanStatic(String value) {
        return TAG_PATTERN.matcher(value
                .replaceAll("(?is)<script.*?</script>", "")
                .replaceAll("(?is)<style.*?</style>", "")
                .replaceAll("(?i)<br\\s*/?>", " ")
                ).replaceAll(" ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#x27;", "'")
                .replace("&#42;", "*")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String frameUrl(OfficialCharacter character) {
        return baseUrl + "/" + capcomSlug(character.slug()) + CAPCOM_FRAME_SUFFIX;
    }

    private String characterProfileUrl(String slug) {
        return baseUrl + "/" + capcomSlug(slug);
    }

    private static String capcomSlug(String slug) {
        return CAPCOM_CHARACTER_SLUGS.getOrDefault(slug, slug);
    }

    private static String internalSlug(String capcomSlug) {
        return INTERNAL_CHARACTER_SLUGS.getOrDefault(capcomSlug, capcomSlug);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private static String slugToName(String slug) {
        return slug.replace('_', ' ').replace('-', ' ').toUpperCase();
    }

    private static Map<String, String> capcomCharacterSlugs() {
        Map<String, String> slugs = new HashMap<>();
        return slugs;
    }

    private static Map<String, String> internalCharacterSlugs() {
        Map<String, String> slugs = new HashMap<>();
        CAPCOM_CHARACTER_SLUGS.forEach((internal, capcom) -> slugs.put(capcom, internal));
        slugs.put("chun-li", "chunli");
        slugs.put("e-honda", "ehonda");
        slugs.put("dee-jay", "deejay");
        slugs.put("a-k-i", "aki");
        slugs.put("akuma", "gouki_akuma");
        slugs.put("m-bison", "vega_mbison");
        slugs.put("c-viper", "cviper");
        return slugs;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record OfficialCharacter(String slug, String name) {
    }

    @FunctionalInterface
    interface OfficialPageClient {
        String fetch(String url);
    }

    public record FrameSyncSummary(int totalCharacters, long successCount, int importedCount,
                                   List<FrameSyncResult> results) {
    }

    public record FrameSyncResult(String slug, String name, int importedCount, boolean success, String error) {
    }
}
