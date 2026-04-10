package com.eqochat.framework.file.client.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilePresignedUrlRespDTO {

    private String uploadUrl;

    private String url;
}
