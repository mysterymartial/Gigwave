package com.gigwave.infrastructure.storage;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.exceptions.*;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import io.imagekit.sdk.models.BaseFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import org.objenesis.ObjenesisStd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageKitFileStorageServiceTest {
    @Mock
    private ImageKit imageKit;
    
    private ImageKitFileStorageService fileStorageService;
    
    private String testUrl = "https://ik.imagekit.io/elq0dgja0/chat-media/test.jpg";
    
    @BeforeEach
    void setUp() throws Exception {
        // Create service instance using Objenesis to bypass constructor
        ObjenesisStd objenesis = new ObjenesisStd();
        fileStorageService = objenesis.newInstance(ImageKitFileStorageService.class);
        
        // Use reflection to inject the mocked ImageKit and set URL endpoint
        Field imageKitField = ImageKitFileStorageService.class.getDeclaredField("imageKit");
        imageKitField.setAccessible(true);
        imageKitField.set(fileStorageService, imageKit);
        
        Field urlEndpointField = ImageKitFileStorageService.class.getDeclaredField("urlEndpoint");
        urlEndpointField.setAccessible(true);
        urlEndpointField.set(fileStorageService, "https://ik.imagekit.io/elq0dgja0");
    }
    
    @Test
    void testStoreChatMedia_Success() throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        Result result = new Result();
        result.setUrl(testUrl);
        
        when(imageKit.upload(any(FileCreateRequest.class))).thenReturn(result);
        
        String url = fileStorageService.storeChatMedia(file, "booking123");
        
        assertNotNull(url);
        assertEquals(testUrl, url);
        verify(imageKit, times(1)).upload(any(FileCreateRequest.class));
    }
    
    @Test
    void testStoreChatMedia_EmptyFile() throws ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);
        
        assertThrows(IOException.class, () -> fileStorageService.storeChatMedia(file, "booking123"));
        verify(imageKit, never()).upload(any(FileCreateRequest.class));
    }
    
    @Test
    void testStoreDisputeEvidence_Success() throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        MultipartFile file = new MockMultipartFile(
                "file", "evidence.pdf", "application/pdf", "test content".getBytes());
        
        Result result = new Result();
        result.setUrl("https://ik.imagekit.io/elq0dgja0/dispute-evidence/test.pdf");
        
        when(imageKit.upload(any(FileCreateRequest.class))).thenReturn(result);
        
        String url = fileStorageService.storeDisputeEvidence(file, "dispute123");
        
        assertNotNull(url);
        assertTrue(url.contains("dispute-evidence"));
        verify(imageKit, times(1)).upload(any(FileCreateRequest.class));
    }
    
    @Test
    void testStoreKycDocument_Success() throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        MultipartFile file = new MockMultipartFile(
                "file", "id.jpg", "image/jpeg", "test content".getBytes());
        
        Result result = new Result();
        result.setUrl("https://ik.imagekit.io/elq0dgja0/kyc-documents/test.jpg");
        
        when(imageKit.upload(any(FileCreateRequest.class))).thenReturn(result);
        
        String url = fileStorageService.storeKycDocument(file, "user123");
        
        assertNotNull(url);
        assertTrue(url.contains("kyc-documents"));
        verify(imageKit, times(1)).upload(any(FileCreateRequest.class));
    }
    
    @Test
    void testDeleteFile_Success() throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException, IllegalAccessException, InstantiationException {
        io.imagekit.sdk.models.results.ResultList resultList = new io.imagekit.sdk.models.results.ResultList();
        BaseFile baseFile = new BaseFile();
        baseFile.setFileId("test-file-id");
        java.util.List<BaseFile> files = new java.util.ArrayList<>();
        files.add(baseFile);
        resultList.setResults(files);
        
        Result deleteResult = new Result();
        
        when(imageKit.getFileList(any())).thenReturn(resultList);
        when(imageKit.deleteFile(anyString())).thenReturn(deleteResult);
        
        assertDoesNotThrow(() -> {
            fileStorageService.deleteFile(testUrl);
        });
    }
    
    @Test
    void testStoreFile_UploadError() throws ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        // ImageKit SDK 2.0.0 throws exceptions on errors, so we mock an exception
        when(imageKit.upload(any(FileCreateRequest.class)))
                .thenThrow(new RuntimeException("Upload failed"));
        
        assertThrows(IOException.class, () -> fileStorageService.storeChatMedia(file, "booking123"));
    }
}
