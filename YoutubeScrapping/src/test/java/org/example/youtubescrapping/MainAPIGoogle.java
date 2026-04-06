package org.example.youtubescrapping;

import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

public class MainAPIGoogle {

    private static final String API_KEY  = "AIzaSyDcMNA6USWny26yGs24xXjRUdyyGd7PmWY";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    public static Set<String> listUsedUrl = ConcurrentHashMap.newKeySet();

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson       gson       = new Gson();


    private static String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429 || response.statusCode() == 403) {
            System.err.println("[WARN] Quota/Rate limit hit, waiting 5s...");
            Thread.sleep(5_000);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " | " + response.body());
        }

        return response.body();
    }

    private static String strSafe(JsonObject obj, String key) {
        JsonElement el = obj == null ? null : obj.get(key);
        return (el == null || el.isJsonNull()) ? "0" : el.getAsString();
    }

    private static int safeParse(String value) {
        if (value == null || value.equalsIgnoreCase("NA")
                || value.equalsIgnoreCase("None")) return 0;
        try {
            return Integer.parseInt(value.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Chuyển ISO 8601 duration (PT1H2M3S) → giây.
     * YouTube trả duration theo định dạng này trong contentDetails.
     */
    private static long parseDuration(String iso) {
        if (iso == null || iso.equals("P0D")) return 0;
        long seconds = 0;
        String s = iso.replace("PT", "");
        if (s.contains("H")) {
            seconds += Long.parseLong(s.substring(0, s.indexOf('H'))) * 3600;
            s = s.substring(s.indexOf('H') + 1);
        }
        if (s.contains("M")) {
            seconds += Long.parseLong(s.substring(0, s.indexOf('M'))) * 60;
            s = s.substring(s.indexOf('M') + 1);
        }
        if (s.contains("S")) {
            seconds += Long.parseLong(s.substring(0, s.indexOf('S')));
        }
        return seconds;
    }

    /**
     * Chuyển ISO 8601 datetime (2024-01-15T10:30:00Z) → Unix timestamp (giây).
     */
    private static long parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) return 0;
        try {
            return java.time.OffsetDateTime
                    .parse(publishedAt)
                    .toInstant()
                    .getEpochSecond();
        } catch (Exception e) {
            return 0;
        }
    }

    // ─── 1. SEARCH CHANNELS ───────────────────────────────────────────────────
    /**
     * Tìm kiếm kênh liên quan đến chủ đề.
     * API: search.list (type=channel)
     *
     * @return Map<channelName, channelId>
     */
    public static HashMap<String, String> queryListChannel(String nameGame, int numberChannel)
            throws IOException, InterruptedException {

        HashMap<String, String> listChannel = new HashMap<>();
        Set<String> seenIds = new HashSet<>();
        String pageToken = "";

        while (listChannel.size() < numberChannel) {
            String url = BASE_URL + "/search"
                    + "?part=snippet"
                    + "&q="          + URLEncoder.encode(nameGame, "UTF-8")
                    + "&type=channel"
                    + "&maxResults=50"
                    + "&key="        + API_KEY
                    + (pageToken.isEmpty() ? "" : "&pageToken=" + pageToken);

            JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
            JsonArray  items = json.getAsJsonArray("items");

            if (items == null || items.isEmpty()) break;

            for (JsonElement el : items) {
                JsonObject item      = el.getAsJsonObject();
                String     channelId = item.getAsJsonObject("id")
                        .get("channelId").getAsString();
                String     name      = item.getAsJsonObject("snippet")
                        .get("channelTitle").getAsString();

                if (!seenIds.contains(channelId)) {
                    listChannel.put(name, channelId);
                    seenIds.add(channelId);
                    System.out.println("ADD channel: " + name + " | " + channelId);
                } else {
                    System.out.println("SKIP channel: " + name + " | " + channelId);
                }

                if (listChannel.size() >= numberChannel) break;
            }

            JsonElement next = json.get("nextPageToken");
            if (next == null || next.isJsonNull()) break;
            pageToken = next.getAsString();
        }

        return listChannel;
    }

    // ─── 2. LIST VIDEOS OF A CHANNEL ─────────────────────────────────────────
    /**
     * Lấy danh sách video ID của kênh liên quan đến từ khoá nameGame.
     * API: search.list (type=video, channelId=...)
     *
     * @return List<videoId>
     */
    public static ArrayList<String> getListVideosChannel(String channelId, String nameGame)
            throws IOException, InterruptedException {

        ArrayList<String> videoIds = new ArrayList<>();

        String url = BASE_URL + "/search"
                + "?part=snippet"
                + "&channelId=" + channelId
                + "&q="         + URLEncoder.encode(nameGame, "UTF-8")
                + "&type=video"
                + "&maxResults=10"
                + "&key="       + API_KEY;

        JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
        JsonArray  items = json.getAsJsonArray("items");

        if (items == null) {
            System.err.println("[WARN] getListVideosChannel: no items for " + channelId);
            return videoIds;
        }

        for (JsonElement el : items) {
            JsonObject idObj  = el.getAsJsonObject().getAsJsonObject("id");
            JsonElement vidEl = idObj.get("videoId");
            if (vidEl == null || vidEl.isJsonNull()) continue;

            String videoId  = vidEl.getAsString();
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

            if (!listUsedUrl.contains(videoUrl)) {
                videoIds.add(videoId);
                listUsedUrl.add(videoUrl);
                System.out.println("ADD video: " + videoUrl);
            } else {
                System.out.println("SKIP Already Exist: " + videoUrl);
            }
        }

        if (videoIds.isEmpty()) {
            System.err.println("[WARN] getListVideosChannel: empty result for channel " + channelId);
        }

        return videoIds;
    }

    // ─── 3. VIDEO STATISTICS ─────────────────────────────────────────────────
    /**
     * Lấy thống kê chi tiết từng video (view, like, comment, duration).
     * API: videos.list — batch tối đa 50 ID mỗi request.
     *
     * @param videoIds  List<videoId>
     * @return          List<Video>
     */
    public static ArrayList<Video> getListDataVideo(ArrayList<String> videoIds)
            throws IOException, InterruptedException {

        ArrayList<Video> listVideo = new ArrayList<>();
        if (videoIds.isEmpty()) {
            System.err.println("[WARN] getListDataVideo: empty list");
            return listVideo;
        }

        // YouTube API cho phép tối đa 50 ID mỗi lần
        List<List<String>> batches = partition(videoIds, 50);

        for (List<String> batch : batches) {
            String ids = String.join(",", batch);
            String url = BASE_URL + "/videos"
                    + "?part=snippet,statistics,contentDetails"
                    + "&id="  + ids
                    + "&key=" + API_KEY;

            JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
            JsonArray  items = json.getAsJsonArray("items");
            if (items == null) continue;

            for (JsonElement el : items) {
                JsonObject obj      = el.getAsJsonObject();
                JsonObject snippet  = obj.getAsJsonObject("snippet");
                JsonObject stats    = obj.getAsJsonObject("statistics");
                JsonObject details  = obj.getAsJsonObject("contentDetails");

                String title       = strSafe(snippet, "title");
                String viewCount   = strSafe(stats,   "viewCount");
                String likeCount   = strSafe(stats,   "likeCount");
                String commentCount= strSafe(stats,   "commentCount");
                String publishedAt = strSafe(snippet, "publishedAt");
                String duration    = String.valueOf(
                        parseDuration(strSafe(details, "duration")));
                String timestamp   = String.valueOf(parsePublishedAt(publishedAt));

                listVideo.add(new Video(title, viewCount, commentCount,
                        likeCount, duration, timestamp));
                System.out.println("Video: " + title
                        + " | views=" + viewCount
                        + " | likes=" + likeCount);
            }
        }

        return listVideo;
    }

    // ─── 4. CHANNEL STATISTICS ────────────────────────────────────────────────
    /**
     * Lấy thống kê kênh + tính avg từ danh sách video đã có.
     * API: channels.list
     *
     * @param channelId   YouTube channel ID (UCxxxx...)
     * @param listVideos  Video đã fetch ở bước trước (dùng để tính avg)
     */
    public static Channel getDataChannel(String channelId, ArrayList<Video> listVideos)
            throws IOException, InterruptedException {

        long nowTime = Instant.now().getEpochSecond();

        String url = BASE_URL + "/channels"
                + "?part=snippet,statistics"
                + "&id="  + channelId
                + "&key=" + API_KEY;

        JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
        JsonArray  items = json.getAsJsonArray("items");

        if (items == null || items.isEmpty()) {
            System.err.println("[WARN] getDataChannel: no data for " + channelId);
            return null;
        }

        JsonObject channel = items.get(0).getAsJsonObject();
        JsonObject snippet = channel.getAsJsonObject("snippet");
        JsonObject stats   = channel.getAsJsonObject("statistics");

        String channelName   = strSafe(snippet, "title");
        String epoch         = String.valueOf(nowTime);
        String followerCount = strSafe(stats,   "subscriberCount");
        String videoCount    = strSafe(stats,   "videoCount");

        // YouTube API không cung cấp trạng thái verified trực tiếp
        // → Dùng "0" (unknown) hoặc bổ sung thêm sau
        String isVerify = "0";

        // ── Tính averages từ listVideos ──────────────────────────────────────
        long totalView = 0, totalLike = 0, totalComment = 0, totalDuration = 0;
        long lastVideoTs = 0;

        for (int i = 0; i < listVideos.size(); i++) {
            Video v = listVideos.get(i);
            totalView    += safeParse(v.getView_count());
            totalLike    += safeParse(v.getLike_count());
            totalComment += safeParse(v.getComment_count());
            totalDuration+= safeParse(v.getDuration());

            if (i == listVideos.size() - 1) {
                lastVideoTs = safeParse(v.getTimestamp());
            }
        }

        int divisor = listVideos.isEmpty() ? 1 : listVideos.size();

        String avgView     = String.valueOf(totalView     / divisor);
        String avgLike     = String.valueOf(totalLike     / divisor);
        String avgComment  = String.valueOf(totalComment  / divisor);
        String avgDuration = String.valueOf(totalDuration / divisor);

        // Frequency = số giây từ video cuối đến hiện tại
        String frequency = lastVideoTs == 0
                ? "0"
                : String.valueOf(nowTime - lastVideoTs);

        System.out.println("Channel: " + channelName
                + " | subs=" + followerCount
                + " | videos=" + videoCount);

        int count = 0;

        for(Video video : listVideos) {
            count += safeParse(video.getView_count());
        }
        String avgViewContentChannel = String.valueOf(count/listVideos.size());

        return new Channel(channelName, epoch, followerCount, videoCount,
                avgView, avgLike, avgComment, avgDuration,
                frequency, isVerify, listVideos, avgViewContentChannel);
    }

    public static void writeFileCSV(String nameFile,
                                    List<Channel> listChannel,
                                    List<String>  avgData) {
        try (FileWriter writer = new FileWriter(nameFile)) {
            writer.append("channel,epoch,followers,video_count,"
                    + "avg10View,avg10Like,avg10Comment,avg10Duration,"
                    + "frequency,isVerify,"
                    + "avgView,avgLikes,avgComments,avgDuration\n");

            for (Channel c : listChannel) {
                writer.append(escape(c.channel)).append(",")
                        .append(c.epoch).append(",")
                        .append(c.channel_follower_count).append(",")
                        .append(c.playlist_count).append(",")
                        .append(c.avgView10Videos).append(",")
                        .append(c.avgLike10Videos).append(",")
                        .append(c.avgComment10Videos).append(",")
                        .append(c.avgDuration10Videos).append(",")
                        .append(c.freequency).append(",")
                        .append(c.isChannelVerify).append(",")
                        .append(avgData.get(0)).append(",")
                        .append(avgData.get(1)).append(",")
                        .append(avgData.get(2)).append(",")
                        .append(avgData.get(3)).append("\n");
            }

            System.out.println("[OK] CSV saved: " + nameFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Chia List thành các batch nhỏ hơn. */
    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String nameGame = "Resident Evil 9";
        String nameFile = "Test2.csv";
        int    numChannels = 1000;

        HashMap<String, String> channelMap = queryListChannel(nameGame, numChannels);
        System.out.println("Found " + channelMap.size() + " channels.");

        AtomicInteger totalViews    = new AtomicInteger(0);
        AtomicInteger totalLikes    = new AtomicInteger(0);
        AtomicInteger totalComments = new AtomicInteger(0);
        AtomicInteger totalDuration = new AtomicInteger(0);
        AtomicInteger totalVideos   = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<CompletableFuture<Channel>> futures = new ArrayList<>();

        int count = 1;
        for (Map.Entry<String, String> entry : channelMap.entrySet()) {
            String channelName = entry.getKey();
            String channelId   = entry.getValue();

            System.out.println(count++ + "/" + channelMap.size()
                    + " queuing: " + channelName + " (" + channelId + ")");

            CompletableFuture<Channel> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ArrayList<String> videoIds =
                            getListVideosChannel(channelId, nameGame);

                    ArrayList<Video> videos = getListDataVideo(videoIds);

                    videos.forEach(v -> {
                        totalViews.addAndGet(safeParse(v.getView_count()));
                        totalLikes.addAndGet(safeParse(v.getLike_count()));
                        totalComments.addAndGet(safeParse(v.getComment_count()));
                        totalDuration.addAndGet(safeParse(v.getDuration()));
                    });
                    totalVideos.addAndGet(videos.size());

                    return getDataChannel(channelId, videos);

                } catch (Exception e) {
                    System.err.println("[ERROR] " + channelName + ": " + e.getMessage());
                    return null;
                }
            }, executor);

            futures.add(future);
        }

        ArrayList<Channel> listChannels = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        executor.shutdown();

        int total = totalVideos.get() == 0 ? 1 : totalVideos.get();
        int avgViews    = totalViews.get()    / total;
        int avgLikes    = totalLikes.get()    / total;
        int avgComments = totalComments.get() / total;
        int avgDuration = totalDuration.get() / total;

        System.out.printf(
                "%nTotal: views=%d likes=%d comments=%d duration=%d videos=%d%n",
                totalViews.get(), totalLikes.get(),
                totalComments.get(), totalDuration.get(), totalVideos.get());
        System.out.printf(
                "Avg  : views=%d likes=%d comments=%d duration=%d%n",
                avgViews, avgLikes, avgComments, avgDuration);

        writeFileCSV(nameFile, listChannels,
                List.of(String.valueOf(avgViews),
                        String.valueOf(avgLikes),
                        String.valueOf(avgComments),
                        String.valueOf(avgDuration)));
    }
}
