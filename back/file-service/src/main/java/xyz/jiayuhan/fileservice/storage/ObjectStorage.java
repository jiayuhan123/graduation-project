package xyz.jiayuhan.fileservice.storage;

import java.io.InputStream;

/** 对象存储抽象，隔离文件业务与阿里云 OSS SDK 的实现细节。 */
public interface ObjectStorage {

    void put(String objectKey, InputStream inputStream, long size, String contentType);

    InputStream open(String objectKey);

    void remove(String objectKey);
}
