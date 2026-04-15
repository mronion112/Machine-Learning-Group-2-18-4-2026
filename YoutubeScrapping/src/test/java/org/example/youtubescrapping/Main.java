package org.example.youtubescrapping;

import com.microsoft.playwright.*;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {

    public static Set<String> listUsedUrl = ConcurrentHashMap.newKeySet();
    public static String coookieFile = "Cookie.json";

    public static HashMap<String, String> queryListChannel(String nameGame, int numberChannel) throws IOException, InterruptedException {
        String delimiter = "###";

        // channel  channelUrl
        HashMap<String,String> listChannel = new HashMap<>();
        HashSet<String> seenUrl = new HashSet<>();
        ProcessBuilder  builder = new ProcessBuilder();
        builder.command(
                "yt-dlp",
                "ytsearch"+numberChannel *10 +":" + nameGame,
                "--flat-playlist",
                "--lazy-playlist",
                "--print", "%(channel)s"+delimiter+"%(channel_url)s",
                "--no-warnings"
//                "--cookies",
//                coookieFile
        );

        builder.redirectErrorStream(false);
        Process process = builder.start();


        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while((line= reader.readLine()) != null){
//            System.out.println(line);
            if(line.length() < 2){
                continue;
            }
            String[] data = line.split(delimiter,2);
            String channelName = data[0].trim();
            String channelUrl = data[1].trim();

            if (!seenUrl.contains(channelUrl)){
                listChannel.put(channelName, channelUrl);
                seenUrl.add(channelUrl);
                System.out.println("ADD: " + channelName + " | " + channelUrl);
            }
            else{
                System.out.println("SKIP: " + channelName + " | " + channelUrl);
            }

            if (listChannel.size() >= numberChannel) {
                process.destroy();
                break;
            }


        }

        int exitCode = process.waitFor();

        System.out.println("Exist with code : " + exitCode);


        return  listChannel;

    }

    public static ArrayList<String> getListVideosChannel(String nameChannel, String nameGame) throws IOException, InterruptedException {
        ArrayList<String> listVideoChannel = new ArrayList<>();

        String delimiter = "###";

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "yt-dlp",
                "ytsearch10:" + nameChannel + " " + nameGame,
                "--flat-playlist",
                "--skip-download",
                "--print",

                "--sleep-interval", "2",
                "--max-sleep-interval", "5",


                "%(uploader)s"+delimiter+"%(webpage_url)s",
                "--no-warnings"
//                "--cookies",
//                coookieFile
        );

        builder.redirectErrorStream(false);
        Process process = builder.start();
        BufferedReader rd = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line ;
        while((line = rd.readLine()) != null) {
            if (line.length() < 2) {
                continue;
            }
            String[] datas = line.split(delimiter, 2);

            if (datas.length < 2) {
                System.out.println("SKIP listDataVideo method line : " + line);
                continue;
            }

            if (!datas[0].trim().equalsIgnoreCase(nameChannel.trim())) {
                System.out.println("SKIP : " + datas[0] + " != " + nameChannel);
                continue;
            }

            if(listUsedUrl.contains(datas[1])){
                System.out.println("SKIP Already Exist : " + datas[0] + " | " + datas[1]);
            }

            else {
                listVideoChannel.add(datas[1]);
                listUsedUrl.add(datas[1]);
                System.out.println("ADD: " + datas[0] + " | " + datas[1] );
            }
        }
        if(listVideoChannel.isEmpty()){
            System.err.println("ERROR getListVideoChannel is empty method line : " + line  );
            return listVideoChannel;
        }
        return listVideoChannel;
    }


    //    yt-dlp "urlVideo" \
//            --print "%(view_count)s|%(comment_count)s|%(like_count)s"
//


    //    yt-dlp "urlChannel"  --print "%(channel)s|%(epoch)s|%(playlist_count)s|%(channel_follower_count)s"





//    x1      Channel Follower Count
//    x2      Epoch
//    x3      Total Videos
//    x8      Avg view 10 videos of channel
//    x9      Avg view 10 videos of channel
//    x10      Avg like 10 videos of channel
//    x11     Avg comment 10 videos of channel
//    x12     Avg Duration 10 videos of Channel
//    x13     Frequency
//    x14     isChannelVerify

    private static int safeParse(String value) {
        if (value == null || value.equalsIgnoreCase("NA") || value.equalsIgnoreCase("None")) return 0;
        try {
            return Integer.parseInt(value.trim().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getVideoCount(String url) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                "--flat-playlist",
                "--print", "%(id)s",
                url
//                "--cookies",
//                coookieFile
        );

        builder.redirectErrorStream(false);
        Process process = builder.start();

        Set<String> ids = new HashSet<>();
        String line;

        try (BufferedReader rb = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            while ((line = rb.readLine()) != null) {
                ids.add(line);
            }
        }

        return String.valueOf(ids.size());
    }

    public static Channel getDataChannel(String urlChannel, ArrayList<Video> listVideos) throws IOException, InterruptedException {
        long nowTime = Instant.now().getEpochSecond();

        String delimiter = "###";

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "yt-dlp", urlChannel,

                "--sleep-interval", "2",
                "--max-sleep-interval", "5",


                "--playlist-end", "10",
                "--print", "%(channel)s" + delimiter + "%(epoch)s" + delimiter + "%(channel_is_verified)s" + delimiter +
                        "%(channel_follower_count)s" + delimiter + "%(title)s" + delimiter + "%(view_count)s" + delimiter +
                        "%(comment_count)s" + delimiter + "%(like_count)s" + delimiter + "%(duration)s" + delimiter + "%(timestamp)s",
                "--no-warnings"
//                "--cookies",
//                coookieFile
        );

        builder.redirectErrorStream(false);
        Process process = builder.start();

        String channelName = "", epoch = "", isChannelVerify = "", followerCount = "";
        long totalView = 0, totalLike = 0, totalComment = 0;
//                totalDuration = 0;
        long lastVideoTs = 0;
        int actualCount = 0;

        try (BufferedReader rb = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = rb.readLine()) != null) {
                System.out.println(line);
                String[] datas = line.split(delimiter);
                if (datas.length < 10) {
                    System.err.println("[WARN] Unexpected line from getDataChannel: " + line);
                    continue;
                }

                if (actualCount == 0) {
                    channelName = datas[0];
                    epoch = datas[1];
                    isChannelVerify = datas[2];
                    followerCount = datas[3];
                }

                totalView += safeParse(datas[5]);
                totalComment += safeParse(datas[6]);
                totalLike += safeParse(datas[7]);
//                totalDuration += safeParse(datas[8]);

                if (actualCount == 8) {   //9
                    lastVideoTs = safeParse(datas[8]);  //9
                }

                actualCount++;
            }
        }

        int divisor = (actualCount == 0) ? 1 : actualCount;

        String avgView = String.valueOf(totalView / divisor);
        String avgLike = String.valueOf(totalLike / divisor);
        String avgComment = String.valueOf(totalComment / divisor);
//        String avgDuration = String.valueOf(totalDuration / divisor);

        String frequency = (lastVideoTs == 0) ? "0" : String.valueOf(nowTime - lastVideoTs);

        isChannelVerify = isChannelVerify.trim().equals("True") ? "1" : "0";
        String playListCount = String.valueOf(177013);
//                getVideoCount(urlChannel);

        int count = 0;

        for(Video video : listVideos) {
            count += safeParse(video.getView_count());
        }
        String avgViewContentChannel = String.valueOf(count/listVideos.size());


        return new Channel(channelName, epoch, followerCount, playListCount,
                avgView, avgLike, avgComment,
//                avgDuration,
                frequency, listVideos, avgViewContentChannel);
    }


    // channel | epoch | playlist_count ! channel_follower_count ||| view_count | comment_count | like_count

//    Biến    Ý nghĩa
//    x1      Channel Follower Count
//    x2      Epoch
//    x3      Total Videos

//    x4      Avg view
//    x5      Avg Comment
//    x6      Avg like
//    x7      Avg Duration
//    x8      timestampCheckContentFresh

//    x9      Avg view 10 videos of channel
//    x10      Avg like 10 videos of channel
//    x11     Avg comment 10 videos of channel
//    x12     Avg Duration 10 videos of Channel
//    x13     Frequency
//    x14     isChannelVerify




    public static ArrayList<Video> getListDataVideo(ArrayList<String> listUrlVideo) throws IOException {
    ArrayList<Video> listVideo = new ArrayList<>();
    String delimiter = "###";

    if(listUrlVideo.isEmpty()) {
        System.err.println("[WARN] getListDataVideo : Empty list url video");
        return listVideo;
    }

    List<String> command = new ArrayList<>(List.of(
            "yt-dlp",

            "--sleep-interval", "2",
            "--max-sleep-interval", "5",


            "--print", "%(title)s"+delimiter+"%(view_count)s"+delimiter+"%(comment_count)s"+delimiter+"%(like_count)s" +
//                                                                                                                    +delimiter+ "%(duration)s"+
                                                                                                                    delimiter+"%(timestamp)s",
            "--no-warnings"
//            "--cookies",
//            coookieFile
    ));
    command.addAll(listUrlVideo);

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(false);
    Process process = builder.start();

    BufferedReader rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = rb.readLine()) != null) {
        if (line.length() < 2) continue;
        String[] datas = line.split(delimiter, 5);

        if (datas.length < 5) {
            System.out.println("SKIP getListDataVideo line: " + line);
            continue;
        }
        listVideo.add(new Video(datas[0], datas[1], datas[2], datas[3], datas[4]));
    }
    return listVideo;
}

//    avgViews   avgLikes  avgComments  avgDuration

    public static void writeFileCSV(String nameFile, List<Channel> listChannel, List<String> avgData){
        try (FileWriter writer = new FileWriter(nameFile)) {
                                                                                                                                                                //,avgDuration
            writer.append("channel,epoch,followers,playlist_count,avg10View,avg10Like,avg10Comment," +
//                    "avg10Duration," +
                    "frequency,isVerify,avgView,avgLikes,avgComments\n");

            for (Channel c : listChannel) {
                writer.append(c.channel).append(",");
                writer.append(c.epoch).append(",");
                writer.append(c.channel_follower_count).append(",");
                writer.append(c.playlist_count).append(",");
                writer.append(c.avgView10Videos).append(",");
                writer.append(c.avgLike10Videos).append(",");
                writer.append(c.avgComment10Videos).append(",");
//                writer.append(c.avgDuration10Videos).append(",");
                writer.append(c.freequency).append(",");
//                writer.append(c.isChannelVerify).append(",");
                writer.append(avgData.get(0)).append(",");
                writer.append(avgData.get(1)).append(",");
                writer.append(avgData.get(2)).append("\n");
//                writer.append(avgData.get(3)).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main() throws IOException, InterruptedException {

        String nameGame = "Học làm bánh";
        String nameFile = "Test1.csv";
        HashMap<String, String> listChannel = queryListChannel(nameGame, 50);

        int count = 1;
        int limit = listChannel.size();
        AtomicInteger totalViews = new AtomicInteger(0);
        AtomicInteger totalLikes = new AtomicInteger(0);
        AtomicInteger totalComments = new AtomicInteger(0);
//        AtomicInteger totalDuration = new AtomicInteger(0);

        AtomicInteger totalVideos = new AtomicInteger(0);


        System.out.println("Scrapping " + listChannel.size() + " channels : " + listChannel.keySet());

        ExecutorService executor = Executors.newFixedThreadPool(12);
        List<CompletableFuture<Channel>> futures = new ArrayList<>();

        for (Map.Entry<String, String> channel : listChannel.entrySet()) {
            System.out.println(count + "/" + limit + " : " + channel.getKey() + " | " + channel.getValue());

            CompletableFuture<Channel> future = CompletableFuture.supplyAsync(() -> {
                try {
                    ArrayList<String> listVideoChannel = getListVideosChannel(channel.getKey(), nameGame);
                    System.out.println("Scrapping Channel : " + channel.getKey() + "\n" + listVideoChannel);

                    ArrayList<Video> listDataVideos = getListDataVideo(listVideoChannel);
                    listDataVideos.forEach(video -> {
                        totalViews.addAndGet(safeParse(video.getView_count()));
                        totalLikes.addAndGet(safeParse(video.getLike_count()));
//                        totalDuration.addAndGet(safeParse(video.getDuration()));
                        totalComments.addAndGet(safeParse(video.getComment_count()));
                    });
                    totalVideos.addAndGet(listDataVideos.size());
                    return getDataChannel(channel.getValue(), listDataVideos);

                } catch (Exception e) {
                    System.err.println("ERROR : " + channel.getKey() + " " + e.getMessage());
                    return null;
                }
            }, executor);

            futures.add(future);
            count++;
        }

        ArrayList<Channel> listChannels = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        executor.shutdown();

        listChannels.forEach(System.out::println);


        System.out.println("Total Views : " + totalViews.get() +
                "\nTotal Like : " + totalLikes.get() +
                "\nTotal Comment : " + totalComments.get() +
//                "\nTotal Duration : " + totalDuration.get() +
                "\nTotal Videos : " + totalVideos.get());

        int avgViews = totalViews.get() / totalVideos.get();
        int avgLikes = totalLikes.get() / totalVideos.get();
        int avgComments = totalComments.get() / totalVideos.get();
//        int avgDuration = totalDuration.get() / totalVideos.get();

        System.out.println("Avg View : " + avgViews +
                "\tAvg Like : " + avgLikes +
                "\tAvg Comment : " + avgComments
//                "\tAvg Duration : " + avgDuration
                );

        List<String> avgData = new ArrayList<>();
        avgData.add(String.valueOf(avgViews));
        avgData.add(String.valueOf(avgLikes));
        avgData.add(String.valueOf(avgComments));
//        avgData.add(String.valueOf(avgDuration));

        writeFileCSV(nameFile, listChannels, avgData);

    }



}
