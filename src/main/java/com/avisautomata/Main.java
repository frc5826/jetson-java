package com.avisautomata;

import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static org.opencv.videoio.Videoio.CAP_GSTREAMER;

public class Main {

    public static void main(String[] args){
        System.out.println("Linking to OpenCV libraries...");
        System.load("/usr/lib/aarch64-linux-gnu/libopencv_java453.so");

        System.out.println("Creating camera info...");
        List<CameraInfo> cInfo = Arrays.asList(
                new CameraInfo(0, 4032, 3040, 30, false),
                new CameraInfo(1, 640, 480, 30, true)
        );
        cInfo.forEach(c -> System.out.println(c));

        System.out.println("Creating VideoCapture cameras...");
        HashMap<CameraInfo, VideoCapture> cameras = new HashMap();
        for(CameraInfo info : cInfo){
            System.out.println("Camera " + info.id + ":");
            System.out.println("\t" + info.getConnectionString());
            cameras.put(info, new VideoCapture(info.getConnectionString(), CAP_GSTREAMER));
        }

        System.out.println("Testing VideoCapture cameras...");
        for(CameraInfo info : cInfo){
            boolean success = false;
            try {
                VideoCapture camera = cameras.get(info);
                //Test read...
                Mat test = new Mat();
                camera.read(test);
                if (camera.isOpened()) {
                    System.out.println("Camera " + info.id + " - Success!");
                    success = true;
                } else {
                    System.out.println("Camera " + info.id + " - Unable to open camera!");
                }
                test.release();
            }
            catch(Exception e){
                System.out.println("Camera " + info.id + " - Failed with error!");
                e.printStackTrace();
            }

            if(!success){
                System.out.println("Removing camera " + info.id + "...");
                cameras.remove(info);
            }
        }

        System.out.println("Writing a test image from each camera...");
        cameras.forEach((info, vc) -> {
            Mat m = new Mat();
            vc.read(m);
            Imgcodecs.imwrite(info.id + ".png", m);
            System.out.println("Created " + info.id + ".png");
            m.release();
        });


        System.out.println("Closing Cameras...");
        for(VideoCapture vc : cameras.values()){
            vc.release();
        }

    }

    private static class CameraInfo {
        public int id;
        public int width;
        public int height;
        public int framerate;
        public boolean yuy2;

        public CameraInfo(int id, int width, int height, int framerate, boolean yuy2) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.framerate = framerate;
            this.yuy2 = yuy2;
        }

        public String getConnectionString(){
            //https://docs.nvidia.com/jetson/l4t/index.html#page/Tegra%20Linux%20Driver%20Package%20Development%20Guide/jetson_xavier_camera_soft_archi.html
            //https://forums.developer.nvidia.com/t/cannot-open-opencv-videocapture-with-gstreamer-pipeline/181639
            if(yuy2){
                //I couldn't get yuy2 working through nvarguscamerasrc, so here's a gross string I came up with.
                return "v4l2src device=/dev/video" + id + " ! video/x-raw, width=(int)" + width + ", height=(int)" + height + ", " +
                        "format=(string)YUY2, framerate=(fraction)" + framerate + "/1 ! nvvidconv ! video/x-raw(memory:NVMM) " +
                        "! nvvidconv ! video/x-raw, format=BGRx ! videoconvert ! video/x-raw, format=BGR ! appsink drop=1";
            }
            else{
                return "nvarguscamerasrc sensor-id=" + id + " ! video/x-raw(memory:NVMM), width=(int)" + width + ", height=(int)"
                        + height + ", format=(string)NV12, framerate=(fraction)" + framerate + "/1 ! nvvidconv ! video/x-raw, format=(string)I420 ! appsink";
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CameraInfo that = (CameraInfo) o;

            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return "CameraInfo{" +
                    "id=" + id +
                    ", width=" + width +
                    ", height=" + height +
                    ", framerate=" + framerate +
                    ", yuy2=" + yuy2 +
                    '}';
        }
    }

}
