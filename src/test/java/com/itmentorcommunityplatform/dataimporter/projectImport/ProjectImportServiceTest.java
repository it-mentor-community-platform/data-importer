package com.itmentorcommunityplatform.dataimporter.projectImport;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileByGithubResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileDetailsDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.service.ProjectImportService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectImportServiceTest {

    @Mock
    GoogleSheetsClient sheetsClient;
    @Mock
    ServiceHttpClient httpClient;
    @Mock
    DataImporterProperties properties;
    @Mock
    Counter projectImportSuccessCounter;
    @Mock
    Counter projectImportErrorCounter;
    @Mock
    Timer projectImportDurationTimer;

    ProjectImportService service;

    @BeforeEach
    void setUp() {
        when(properties.getSheetRangeProjects()).thenReturn("dummy-range");

        service = new ProjectImportService(
                sheetsClient,
                httpClient,
                properties,
                projectImportSuccessCounter,
                projectImportErrorCounter,
                projectImportDurationTimer
        );
    }

    @Test
    void testDoImportPrivate() throws Exception {
        List<List<Object>> rows = List.of(
                List.of("Июнь, 2021", "simulation", "Kotlin", "", "https://github.com/anelfer/simulation2077", "", "https://github.com/anelfer"),
                List.of("Ноябрь, 2021", "simulation", "PHP", "", "https://github.com/BorBoris23/WarhammerWB", "", "https://github.com/BorBoris23"),
                List.of("Апрель, 2022", "tennis-scoreboard", "Java", "", "https://github.com/Jollykai/tennisTableboard", "", "https://github.com/Jollykai")
        );
        when(sheetsClient.readSheet("dummy-range")).thenReturn(rows);

        when(httpClient.getProfileByGithubUrl("https://github.com/anelfer"))
                .thenReturn(new ProfileByGithubResponseDto(1L,
                        new ProfileDetailsDto("https://github.com/anelfer", "https://t.me/user1")));
        when(httpClient.getProfileByGithubUrl("https://github.com/BorBoris23"))
                .thenReturn(new ProfileByGithubResponseDto(2L,
                        new ProfileDetailsDto("https://github.com/BorBoris23", "https://t.me/user2")));
        when(httpClient.getProfileByGithubUrl("https://github.com/Jollykai"))
                .thenReturn(new ProfileByGithubResponseDto(3L,
                        new ProfileDetailsDto("https://github.com/Jollykai", "https://t.me/user3")));

        Method doImportMethod = ProjectImportService.class.getDeclaredMethod("doImport");
        doImportMethod.setAccessible(true);
        doImportMethod.invoke(service);

        verify(projectImportSuccessCounter, times(3)).increment();
        verify(projectImportErrorCounter, never()).increment();

        verify(httpClient).getProfileByGithubUrl("https://github.com/anelfer");
        verify(httpClient).getProfileByGithubUrl("https://github.com/BorBoris23");
        verify(httpClient).getProfileByGithubUrl("https://github.com/Jollykai");
    }
}
