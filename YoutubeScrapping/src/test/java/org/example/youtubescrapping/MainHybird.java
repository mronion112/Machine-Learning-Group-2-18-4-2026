package org.example.youtubescrapping;

import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;

public class MainHybird {

//    Biến    Mean
//    x1      Channel Follower Count
//    x2      Epoch
//    x3      Total Videos

//    x4      predict_view
//    x5      predict_like
//    x6      predict_comment

//    x7      Avg view 10 videos of channel
//    x8      Avg like 10 videos of channel
//    x9     Avg comment 10 videos of channel
//    x10     Avg Duration 10 videos of Channel
//    x11     Frequency
//    x12     isChannelVerify

    private static final String API_KEY  = "";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";
    private static final Long timeNow = Instant.now().getEpochSecond();


    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static Set<String> listUsedUrl = ConcurrentHashMap.newKeySet();


    public static HashMap<String, String> queryListChannel(String nameGame, int numberChannel)
            throws IOException, InterruptedException {

        String delimiter = "###";
        HashMap<String, String> listChannel = new HashMap<>();
        HashSet<String> seenUrl = new HashSet<>();
        String nameGameEncoded = nameGame.replace(" ", "+");
        String searchUrl = "https://www.youtube.com/results?search_query=" + nameGameEncoded + "channel&sp=EgIQAg%3D%3D";

        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                searchUrl,
                "--flat-playlist",
                "--print", "%(channel)s" + delimiter + "%(channel_url)s",
                "--playlist-items", "1:" + numberChannel*10,
                "--no-warnings"
        );
        builder.redirectErrorStream(false);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 2) continue;
                String[] data = line.split(delimiter, 2);
                if (data.length < 2) continue;

                String channelName = data[0].trim();
                String channelUrl  = data[1].trim();

                if (!seenUrl.contains(channelUrl)) {
                    listChannel.put(channelName, channelUrl);
                    seenUrl.add(channelUrl);
                    System.out.println("ADD: " + channelName + " | " + channelUrl);
                } else {
                    System.out.println("SKIP: " + channelName + " | " + channelUrl);
                }

                if (listChannel.size() >= numberChannel) {
                    process.destroy();
                    break;
                }
            }
        }

        process.waitFor();
        System.out.println("queryListChannel done: " + listChannel.size() + " channels");
        return listChannel;
    }


    public static ArrayList<String> getListVideosChannel(String channelUrl, String nameGame)
            throws IOException, InterruptedException {

        ArrayList<String> listVideoUrls = new ArrayList<>();

        // Tìm video theo keyword trong đúng kênh đó
        String searchUrl = channelUrl + "/search?query="
                + URLEncoder.encode(nameGame, "UTF-8");

        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                searchUrl,
                "--flat-playlist",
                "--playlist-end", "10",
                "--sleep-interval",     "3",
                "--max-sleep-interval", "5",
                "--print", "%(webpage_url)s",
                "--no-warnings"
        );
        builder.redirectErrorStream(false);
        Process process = builder.start();

        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.length() < 5 || !line.startsWith("http")) continue;

                if (!listUsedUrl.contains(line)) {
                    listVideoUrls.add(line);
                    listUsedUrl.add(line);
                    System.out.println("ADD video: " + line);
                } else {
                    System.out.println("SKIP duplicate: " + line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (listVideoUrls.isEmpty()) {
            System.err.println("[WARN] getListVideosChannel empty: "
                    + channelUrl + " (exit=" + exitCode + ")");
        }

        return listVideoUrls;
    }

    // Youtube API V3 : 1 quota (max 50 IDs)
    public static ArrayList<Video> getListDataVideo(ArrayList<String> videoUrls)
            throws IOException, InterruptedException {

        ArrayList<Video> listVideo = new ArrayList<>();
        if (videoUrls.isEmpty()) {
            System.err.println("[WARN] getListDataVideo: empty list");
            return listVideo;
        }

        List<String> videoIds = videoUrls.stream()
                .map(MainHybird::extractVideoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            System.err.println("[WARN] getListDataVideo: list ID video is Empty");
            return listVideo;
        }

        for (List<String> batch : partition(videoIds, 50)) {
            String ids = String.join(",", batch);
            String url = BASE_URL + "/videos"
                    + "?part=snippet,statistics,contentDetails"
                    + "&id="  + ids
                    + "&key=" + API_KEY;

            JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
            JsonArray  items = json.getAsJsonArray("items");
            if (items == null) continue;

            for (JsonElement el : items) {
                JsonObject obj = el.getAsJsonObject();
                JsonObject snippet = obj.getAsJsonObject("snippet");
                JsonObject stats = obj.getAsJsonObject("statistics");
                JsonObject details = obj.getAsJsonObject("contentDetails");

                String title = strSafe(snippet,"title");
                String viewCount = strSafe(stats,"viewCount");
                String likeCount = strSafe(stats,"likeCount");
                String commentCount = strSafe(stats,"commentCount");
//                String duration = String.valueOf(parseDuration(strSafe(details, "duration")));
                String timestamp = String.valueOf(
                        parsePublishedAt(strSafe(snippet, "publishedAt")));

//                timestamp = String.valueOf(timeNow - Long.parseLong(timestamp));

                listVideo.add(new Video(title, viewCount, commentCount,
                        likeCount,
//                        duration,
                        timestamp));
                System.out.println("  [video] " + title
                        + " | views=" + viewCount + " | likes=" + likeCount);
            }
        }

        return listVideo;
    }


    public static Channel getDataChannel(String channelUrl, ArrayList<Video> listVideos)
            throws IOException, InterruptedException {

//        long nowTime = Instant.now().getEpochSecond();

        String channelId = resolveChannelId(channelUrl);
        if (channelId == null) {
            System.err.println("[WARN] Cannot resolve channelId: " + channelUrl);
            return null;
        }

        // ── Gọi API: thêm "brandingSettings" để lấy isVerify ────────────────────
        // Quota: vẫn 1 unit, chỉ thêm part
        String url = BASE_URL + "/channels"
                + "?part=snippet,statistics,status"
                + "&id="  + channelId
                + "&key=" + API_KEY;

        JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
        JsonArray  items = json.getAsJsonArray("items");

        if (items == null || items.isEmpty()) {
            System.err.println("[WARN] getDataChannel: no data for " + channelId);
            return null;
        }

        JsonObject ch      = items.get(0).getAsJsonObject();
        JsonObject snippet = ch.getAsJsonObject("snippet");
        JsonObject stats   = ch.getAsJsonObject("statistics");
        JsonObject status  = ch.getAsJsonObject("status");

        String channelName   = strSafe(snippet, "title");
        String publishedAtRaw = strSafe(snippet, "publishedAt"); // Định dạng ISO 8601 (VD: 2015-05-20T12:00:00Z)
        long publishedAtEpoch = parsePublishedAt(publishedAtRaw);

    // Tính "Tuổi của kênh" tính bằng giây từ lúc lập đến thời điểm hiện tại
        String epoch = String.valueOf(timeNow - publishedAtEpoch);
        String followerCount = strSafe(stats, "subscriberCount");
        String videoCount    = strSafe(stats, "videoCount");

        // isVerify: "longUploadsStatus" = "allowed" chỉ dành cho kênh đã verify
        // Đây là cách gần nhất API v3 cho phép — không có field "verified" trực tiếp
        // "allowed"      → kênh đã verify (upload được video > 15 phút)
        // "eligible"     → chưa verify
        // "disallowed"   → bị hạn chế
        String longUploadStatus = strSafe(status, "longUploadsStatus");
        String isVerify = longUploadStatus.equals("allowed") ? "1" : "0";

        // ── avgViewContentChannel: avg view các video content này trong kênh ─────
        long totalView = 0, totalLike = 0, totalComment = 0;
//                totalDuration = 0;

        for (Video v : listVideos) {
            totalView     += safeParse(v.getView_count());
            totalLike     += safeParse(v.getLike_count());
            totalComment  += safeParse(v.getComment_count());
//            totalDuration += safeParse(v.getDuration());
        }

        int divisor = listVideos.isEmpty() ? 1 : listVideos.size();
        String avgViewContentChannel = String.valueOf(totalView    / divisor); // avg view content này
        String avgLike               = String.valueOf(totalLike    / divisor);
        String avgComment            = String.valueOf(totalComment / divisor);
//        String avgDuration           = String.valueOf(totalDuration/ divisor);


        List<Long> timestamps = listVideos.stream()
                .map(v -> (long) safeParse(v.getTimestamp()))
                .filter(ts -> ts > 0)
                .sorted()
                .collect(Collectors.toList());

        String frequency;
        if (timestamps.size() < 2) {
            frequency = "0";
        } else {
            long span      = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
            long avgGapSec = span / (timestamps.size() - 1);
            frequency = String.valueOf(avgGapSec);
        }

        System.out.println("[channel] " + channelName
                + " | subs="    + followerCount
                + " | videos="  + videoCount
                + " | verify="  + isVerify
                + " | freq="    + frequency + "s"
                + " | avgView=" + avgViewContentChannel);

        return new Channel(channelName, epoch, followerCount, videoCount,
                avgViewContentChannel, avgLike, avgComment,
//                avgDuration,
                frequency, isVerify, listVideos, avgViewContentChannel);
    }


    public static void writeFileCSV(String nameFile,
                                    List<Channel> listChannel,
                                    List<String>  avgData) {
        try (FileWriter writer = new FileWriter(nameFile)) {
            writer.append("channel,epoch,followers,video_count,"
                                                        //avg10Duration,
                    + "avg10View,avg10Like,avg10Comment,"
                    + "frequency,isVerify,"                     //avgDuration
                    + "predict_view,predict_like,predict_comment,view\n");


            for (Channel c : listChannel) {
                System.out.println(c);
                String predict_view = "";
                String predict_like = "";
                String predict_comment = "";

                try {
                    predict_view = String.valueOf(Long.parseLong(avgData.get(0)) * Long.parseLong(c.getAvgView10Videos()) / Long.parseLong(c.getChannel_follower_count()));
                    predict_like = String.valueOf(Long.parseLong(avgData.get(1)) * Long.parseLong(c.getAvgLike10Videos()) / Long.parseLong(c.getChannel_follower_count()));
                    predict_comment = String.valueOf(Long.parseLong(avgData.get(2)) * Long.parseLong(c.getAvgComment10Videos()) / Long.parseLong(c.getChannel_follower_count()));
//                String predicted_duration = String.valueOf(Long.parseLong(avgData.get(3)) * Long.parseLong(c.getAvgDuration10Videos())/Long.parseLong(c.getChannel_follower_count()));
                }
                catch (NumberFormatException e) {
                    System.out.println("ERROR add channel " + c.getChannel() + e.getMessage());
                    continue;
                }
                catch (ArithmeticException e){
                    System.out.println("ERROR add channel " + c.getChannel() + e.getMessage());
                    continue;

                }
                catch (Exception e){
                    System.out.println("ERROR add channel " + c.getChannel() + e.getMessage());
                    continue;

                }
                writer.append(csvEscape(c.channel)).append(",")
                        .append(c.epoch).append(",")
                        .append(c.channel_follower_count).append(",")
                        .append(c.playlist_count).append(",")
                        .append(c.avgView10Videos).append(",")
                        .append(c.avgLike10Videos).append(",")
                        .append(c.avgComment10Videos).append(",")
//                        .append(c.avgDuration10Videos).append(",")
                        .append(c.freequency).append(",")
                        .append(c.isChannelVerify).append(",")
//                        .append(avgData.get(0)).append(",")
//                        .append(avgData.get(1)).append(",")
//                        .append(avgData.get(2)).append(",")
//                        .append(avgData.get(3)).append(",")
                        .append(predict_view).append(",")
                        .append(predict_like).append(",")
                        .append(predict_comment).append(",")
                        .append(c.avgViewContentChannel).append("\n");
            }

            System.out.println("[OK] CSV saved: " + nameFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = Pattern.compile(
                "(?:watch\\?v=|youtu\\.be/|/shorts/)([A-Za-z0-9_-]{11})"
        ).matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static String resolveChannelId(String channelUrl)
            throws IOException, InterruptedException {

        if (channelUrl == null) return null;

        // /channel/UCxxxx → trực tiếp, 0 quota
        Matcher m = Pattern.compile("/channel/(UC[A-Za-z0-9_-]+)").matcher(channelUrl);
        if (m.find()) return m.group(1);

        // /@Handle → channels.list?forHandle, 1 quota
        Matcher h = Pattern.compile("/@([A-Za-z0-9_.-]+)").matcher(channelUrl);
        if (h.find()) {
            String handle = h.group(1);
            System.out.println("[INFO] Resolving @" + handle + " (1 quota)");
            String url = BASE_URL + "/channels"
                    + "?part=id&forHandle=" + handle
                    + "&key=" + API_KEY;
            JsonObject json  = JsonParser.parseString(httpGet(url)).getAsJsonObject();
            JsonArray  items = json.getAsJsonArray("items");
            if (items != null && !items.isEmpty())
                return items.get(0).getAsJsonObject().get("id").getAsString();
        }

        System.err.println("[WARN] Cannot resolve: " + channelUrl);
        return null;
    }

    private static String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429 || response.statusCode() == 403) {
            System.err.println("[WARN] Rate limit, retry after 5s...");
            Thread.sleep(5_000);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode() + " | " + response.body());

        return response.body();
    }

    private static String strSafe(JsonObject obj, String key) {
        JsonElement el = (obj == null) ? null : obj.get(key);
        return (el == null || el.isJsonNull()) ? "0" : el.getAsString();
    }

    private static int safeParse(String value) {
        if (value == null || value.equalsIgnoreCase("NA")
                || value.equalsIgnoreCase("None")) return 0;
        try {
            return Integer.parseInt(value.trim().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) { return 0; }
    }

    private static long parseDuration(String iso) {
        if (iso == null || iso.equals("P0D")) return 0;
        long sec = 0;
        String s = iso.replace("PT", "");
        if (s.contains("H")) { sec += Long.parseLong(s.substring(0, s.indexOf('H'))) * 3600; s = s.substring(s.indexOf('H') + 1); }
        if (s.contains("M")) { sec += Long.parseLong(s.substring(0, s.indexOf('M'))) * 60;   s = s.substring(s.indexOf('M') + 1); }
        if (s.contains("S")) { sec += Long.parseLong(s.substring(0, s.indexOf('S'))); }
        return sec;
    }

    private static long parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty() || publishedAt.equals("0")) return 0;
        try {
            return java.time.OffsetDateTime.parse(publishedAt).toInstant().getEpochSecond();
        } catch (Exception e) { return 0; }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size)
            result.add(list.subList(i, Math.min(i + size, list.size())));
        return result;
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        return (value.contains(",") || value.contains("\""))
                ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String nameGame    = "Genshin impact";
        String nameFile    = nameGame + ".csv";
        int    numChannels = 200;

        HashMap<String, String> channelMap = queryListChannel(nameGame, numChannels);
        System.out.println("Scraping " + channelMap.size() + " channels...");

        AtomicLong totalViews    = new AtomicLong(0);
        AtomicLong totalLikes    = new AtomicLong(0);
        AtomicLong totalComments = new AtomicLong(0);
//        AtomicInteger totalDuration = new AtomicInteger(0);
        AtomicInteger totalVideos   = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<CompletableFuture<Channel>> futures = new ArrayList<>();

        int idx = 1;
        for (Map.Entry<String, String> entry : channelMap.entrySet()) {
            String channelName = entry.getKey();
            String channelUrl  = entry.getValue();

            System.out.println(idx++ + "/" + channelMap.size()
                    + " queuing: " + channelName + " | " + channelUrl);

            CompletableFuture<Channel> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // [yt-dlp] Bước 2: Dùng channelUrl trực tiếp — 0 quota, FIX bug --print
                    ArrayList<String> videoUrls = getListVideosChannel(channelUrl, nameGame);

                    // [API]   Bước 3: Stats video — 1 quota/batch
                    ArrayList<Video> videos = getListDataVideo(videoUrls);

                    videos.forEach(v -> {
                        totalViews.addAndGet(safeParse(v.getView_count()));
                        totalLikes.addAndGet(safeParse(v.getLike_count()));
                        totalComments.addAndGet(safeParse(v.getComment_count()));
//                        totalDuration.addAndGet(safeParse(v.getDuration()));
                    });
                    totalVideos.addAndGet(videos.size());

                    // [API]   Bước 4: Stats kênh — 1 quota
                    return getDataChannel(channelUrl, videos);

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

        int total = Math.max(totalVideos.get(), 1);
        List<String> avgData = List.of(
                String.valueOf(totalViews.get()    / total),
                String.valueOf(totalLikes.get()    / total),
                String.valueOf(totalComments.get() / total)
//                String.valueOf(totalDuration.get() / total)
        );

        System.out.printf("%nAvg: views=%s likes=%s comments=%s ",
//                        "duration=%s%n",
                avgData.get(0), avgData.get(1), avgData.get(2));
//                avgData.get(3));

        writeFileCSV(nameFile, listChannels, avgData);
    }
}