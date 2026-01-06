package uk.ac.ed.eci.libCZI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.eci.libCZI.document.DocumentInfo;
import uk.ac.ed.eci.libCZI.document.DimensionInfo;
import uk.ac.ed.eci.libCZI.document.GeneralDocumentInfo;
import uk.ac.ed.eci.libCZI.document.ScalingInfo;
import uk.ac.ed.eci.libCZI.document.AvailableDimensions;


public class DocumentInfoTest {
    private static final Path TEST_IMAGE_PATH = Paths.get("test-images", "test-image.czi");
    private CziStreamReader reader;
    private CZIInputStream streamResult;

    @BeforeEach
    public void setup() {
        assertTrue(Files.exists(TEST_IMAGE_PATH), "Test image should have been downloaded by Maven.");

        streamResult = CZIInputStream.createInputStreamFromFileUTF8(TEST_IMAGE_PATH.toString());
        reader = CziStreamReader.fromStream(streamResult);        
    }

    @AfterEach
    public void teardown() throws Exception {
        reader.close();
        streamResult.close();        
    }

    @Test
    public void testOpenDocumentInfo() {
        DocumentInfo documentInfo = reader.metadata().documentInfo();
        assertNotNull(documentInfo, "Document info should not be null.");
    }

    @Test
    public void testGeneralDocumentInfo() {
        DocumentInfo documentInfo = reader.metadata().documentInfo();
        GeneralDocumentInfo generalDocumentInfo = documentInfo.generalDocumentInfo();

        // CZIcmd -s /images/test-image.czi -c "PrintInformation" -i "GeneralInfo"
        assertNotNull(generalDocumentInfo, "General document info should not be null.");
        assertEquals("zeiss", generalDocumentInfo.username());

        // Compare the Instant on the timeline, which is robust against timezone/offset differences.
        Instant expectedInstant = OffsetDateTime.parse("2022-10-05T10:06:46.2913112-05:00").toInstant();
        Instant actualInstant = generalDocumentInfo.creationDateTime().toInstant();
        assertEquals(expectedInstant, actualInstant);
    }

    @Test
    public void testScalingInfo() {
        DocumentInfo documentInfo = reader.metadata().documentInfo();
        ScalingInfo scalingInfo = documentInfo.scalingInfo();
        assertNotNull(scalingInfo, "Scaling info should not be null.");

        // Values confirmed by inspecting the test image's metadata.
        // CZIcmd -s /images/test-image.czi -c "PrintInformation" -i "ScalingInfo"
        assertTrue(scalingInfo.scaleX().isPresent(), "Scale X should be present.");
        assertEquals(3.45915e-07, scalingInfo.scaleX().get(), 1e-12);
        assertTrue(scalingInfo.scaleY().isPresent(), "Scale Y should be present.");
        assertEquals(3.45915e-07, scalingInfo.scaleY().get(), 1e-12);
        assertFalse(scalingInfo.scaleZ().isPresent(), "Scale Z should not be present (NaN).");
    }

    @Test
    public void testAvailableDimensions() {
        DocumentInfo documentInfo = reader.metadata().documentInfo();
        AvailableDimensions availableDimensions = documentInfo.availableDimensions();
        assertNotNull(availableDimensions, "Dimension info should not be null.");
        assertEquals(availableDimensions.toArray()[0], 2, 1e-12);
    }

    @Test
    public void testDimensionInfo() {
        DocumentInfo documentInfo = reader.metadata().documentInfo();
        DimensionInfo dimensionInfo = documentInfo.dimensionInfo(2);
        assertNotNull(dimensionInfo, "Dimension info should not be null.");
        String string = dimensionInfo.getJSONString();
        assertTrue(!string.isEmpty(), "JSON string should not be empty.");
    }
}
