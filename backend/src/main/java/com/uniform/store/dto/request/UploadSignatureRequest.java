package com.uniform.store.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadSignatureRequest {

    @Size(max = 150)
    @Pattern(regexp = "^[a-zA-Z0-9_/-]*$", message = "Folder may contain letters, digits, '-', '_', '/' only")
    private String folder;

    @Size(max = 120)
    private String filenameHint;
}
