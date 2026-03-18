package uk.ac.ed.eci.libCZI;
    
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ed.eci.libCZI.document.DocumentInfo;
import uk.ac.ed.eci.libCZI.metadata.Metadata;

public class CziStreamReaderTest {
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
    public void testOpenReaderFromFileStream() {
        assertNotNull(reader, "Reader handle should not be null.");
    }

    @Test
    public void testSubBlockCount() {
        SubBlockStatistics stats = reader.simpleReaderStatistics();
        assertEquals(150, stats.subBlockCount());  
    }

    @Test
    public void testBoundingBoxes() {
        SubBlockStatistics stats = reader.simpleReaderStatistics();
        IntRect bb = stats.boundingBox();
        IntRect bb0 = stats.boundingBoxLayer0();

        assertEquals(-123977, bb.x());
        assertEquals(28824, bb.y());
        assertEquals(50171, bb.w());
        assertEquals(11349, bb.h());

        // Layer 0 BB
        assertEquals(-123977, bb0.x());
        assertEquals(28824, bb0.y());
        assertEquals(50159, bb0.w());
        assertEquals(11338, bb0.h());
    }

    @Test
    public void testMIndex() {
        SubBlockStatistics stats = reader.simpleReaderStatistics();
        
        assertEquals(0, stats.minMIndex());
        assertEquals(26, stats.maxMIndex());
    }

    @Test
    public void testAttachmentCount() {
        int attachmentCount = reader.attachmentCount();

        assertEquals(6, attachmentCount);
    }

    @Test
    public void testAttachmentInfo() {
        AttachmentInfo[] attachments = reader.getAttachments();

        assertNotNull(attachments, "Attachments should not be null.");
        assertEquals(6, attachments.length);

        AttachmentInfo fileDetails = attachments[0];
        assertEquals(UUID.fromString("7C31DD48-5555-42F3-9225-4881DA8047F6"), fileDetails.guid());
        assertEquals("CZEVL", fileDetails.contentFileType());
        assertEquals("EventList", fileDetails.name());

        AttachmentInfo timeStamps = attachments[1];
        assertEquals(UUID.fromString("BD7D4E6F-B2A2-420C-BBD1-9282B8A89EE0"), timeStamps.guid());
        assertEquals("CZTIMS", timeStamps.contentFileType());
        assertEquals("TimeStamps", timeStamps.name());

        AttachmentInfo label = attachments[2];
        // TODO: The CLI tool reports this as: BF5AC657-B428-4071-9125-5341B84EF870 find out why!
        assertEquals(UUID.fromString("BF5AC657-B428-4071-9125-5341B84EF807"), label.guid());
        assertEquals("CZI", label.contentFileType());
        assertEquals("Label", label.name());

        AttachmentInfo slidePreview = attachments[3];
        assertEquals(UUID.fromString("F8A7F166-1692-4E5B-B881-432695115959"), slidePreview.guid());
        assertEquals("CZI", slidePreview.contentFileType());
        assertEquals("SlidePreview", slidePreview.name());

        AttachmentInfo profile = attachments[4];
        assertEquals(UUID.fromString("DF9C3F55-5844-4171-B234-8F69AFAB7F92"), profile.guid());
        assertEquals("Zip-Comp", profile.contentFileType());
        assertEquals("Profile", profile.name());

        AttachmentInfo thumbnail = attachments[5];
        assertEquals(UUID.fromString("6B28C19C-E75A-468D-935A-16A3194B3550"), thumbnail.guid());
        assertEquals("JPG", thumbnail.contentFileType());
        assertEquals("Thumbnail", thumbnail.name());
    }

    @Test
    public void testPyramidStatistics() {
        ScenePyramidStatistics stats = reader.pyramidStatistics();
        assertNotNull(stats, "Pyramid statistics should not be null.");

        List<PyramidLayerStats> stats0 = stats.getStatsByScene().get("0");
        PyramidLayerStats stats00 = stats0.get(0);
        assertEquals(0, stats00.getLayerInfo().getMinificationFactor());
        assertEquals(0, stats00.getLayerInfo().getPyramidLayerNo());
        assertEquals(27, stats00.getCount());
    }

    @Test
    public void testMetadataSegment() {
        Metadata metadata = reader.metadata();
        assertNotNull(metadata, "Metadata should not be null.");
    }

    @Test
    public void testDimensionBounds() {
        SubBlockStatistics subBlockStatistics = reader.simpleReaderStatistics();
        DimBounds dimBounds = subBlockStatistics.dimBounds();
        assertNotNull(dimBounds, "DimBounds should not be null.");
        assertEquals(18, dimBounds.dimensionsValid());
        assertEquals(LibCziFFM.K_MAX_DIMENSION_COUNT, dimBounds.size().length, "DimBounds size should be K_MAX_DIMENSION_COUNT.");
        assertEquals(LibCziFFM.K_MAX_DIMENSION_COUNT, dimBounds.start().length, "DimBounds size should be K_MAX_DIMENSION_COUNT.");
    }
}
