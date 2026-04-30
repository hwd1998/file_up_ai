package com.example.upload.common.constants;

public final class UploadConstants {

    private UploadConstants() {}

    public static final long CHUNK_SIZE = 5 * 1024 * 1024L;
    public static final long MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024L;
    public static final int MAX_FILES_PER_REQUEST = 40;
    public static final int FILE_EXPIRE_DAYS = 90;
    public static final int MAX_RETRY_COUNT = 3;
    public static final long MAX_ROWS = 1_000_000L;

    public static final String SESSION_LOGIN_USER = "loginUser";

    public static final String PERM_UPLOAD = "upload";
    public static final String PERM_VIEW_HISTORY = "view_history";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";
}
