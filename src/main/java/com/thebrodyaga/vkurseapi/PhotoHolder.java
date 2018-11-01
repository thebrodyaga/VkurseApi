package com.thebrodyaga.vkurseapi;

import com.sun.istack.internal.Nullable;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.fave.responses.GetPhotosResponse;
import com.vk.api.sdk.objects.photos.Photo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PhotoHolder {
    private Integer offset = 0;
    private Integer totalCount = 0;
    private File directory;
    private File errorFile;

    public PhotoHolder() {
        directory = new File("/Users/admin/Desktop/VkImages");
        if (!directory.isDirectory()) {
            directory.mkdir();
        }
        errorFile = new File(directory, "errorFile.txt");
        try {
            errorFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startLoad() {
        while (true) {
            List<Photo> photoMass = getPhotoMass();
            if (photoMass.isEmpty()) break;
            ExecutorService service = Executors.newCachedThreadPool();
            photoMass.forEach((photo -> service.submit(() -> loadPhoto(photo))));
            try {
                service.shutdown();
                service.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("completed");
    }

    private void loadPhoto(Photo photo) {
        String largestSizeUrl = getLargestSizeUrl(photo);
        if (largestSizeUrl == null)
            return;
        try (FileWriter fw = new FileWriter(errorFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new URL(largestSizeUrl));
                String format = getFileFormat(largestSizeUrl);
                ImageIO.write(bufferedImage, format, new File(directory, photo.getId().toString() + "." + format));
            } catch (IOException e) {
                out.println(String.format("photoId = %s Ошибка скачивания Ошибка: %s\n", photo.getId(), e.getMessage()));
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileFormat(String largestSizeUrl) {
        int i = largestSizeUrl.lastIndexOf('.');
        String extension = "";
        if (i > 0) {
            extension = largestSizeUrl.substring(i + 1);
        }
        return extension;
    }

    @Nullable
    private String getLargestSizeUrl(Photo photo) {
        if (photo.getPhoto2560() != null)
            return photo.getPhoto2560();

        if (photo.getPhoto1280() != null)
            return photo.getPhoto1280();

        if (photo.getPhoto807() != null)
            return photo.getPhoto807();

        if (photo.getPhoto604() != null)
            return photo.getPhoto604();

        if (photo.getPhoto130() != null)
            return photo.getPhoto130();

        if (photo.getPhoto75() != null)
            return photo.getPhoto75();

        return null;
    }

    private List<Photo> getPhotoMass() {
        ArrayList<Photo> photos = new ArrayList<>();
        if (offset > totalCount) return photos;
        try {
            GetPhotosResponse photosResponse = Application.getVk()
                    .fave()
                    .getPhotos(Application.getPhotoUserActor())
                    .offset(offset != null ? offset : 0)
                    .execute();
            totalCount = photosResponse.getCount();
            offset = offset + photosResponse.getItems().size();
            photos.addAll(photosResponse.getItems());
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }
        return photos;
    }
}
