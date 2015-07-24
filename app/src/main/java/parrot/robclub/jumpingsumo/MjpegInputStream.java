package parrot.robclub.jumpingsumo;

/**
 * Author: nguyenquockhai create on 23/07/2015 at Robotics Club.
 * Desc:
 */
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public class MjpegInputStream extends DataInputStream {
    protected int mSequence = 0;
    protected int mContentLength = -1;
    protected boolean isContentLengthAvailable = false;
    protected boolean isFirstPass = true;

    /**
     * Wrap the given input stream with the MjpegInputStream.  Internal buffers
     * are created automatially.
     *
     * @param in stream to read and parse MJPEG frames from
     */
    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, MjpegFormat.FRAME_MAX_LENGTH));
    }

    /**
     * Read the next MjpegFrame from the stream.
     *
     * @return the next MJPEG frame.
     * @throws IOException if there is an error while reading data
     */
    public MjpegFrame readMjpegFrame() throws IOException {
        //mark the start of the frame
        mark(MjpegFormat.FRAME_MAX_LENGTH);

        //get length of header
        int headerLen = MjpegFormat.getStartOfSequence(this,
                JpegFormat.SOI_MARKER); //position of first byte of the jpeg

        if (isFirstPass) {
            //attempt to parse content length
            isFirstPass = false; //do this once
            reset();

            byte[] header = new byte[headerLen];
            readFully(header);

            try {
                mContentLength = MjpegFormat.parseContentLength(header);
                isContentLengthAvailable = true; //flag for more efficientcy
            } catch (NumberFormatException nfe) {
                isContentLengthAvailable = false;
            }
        }

        reset();

        if (isContentLengthAvailable) {
            //the fast way
            byte[] header = new byte[headerLen];
            readFully(header);

            try {
                mContentLength = MjpegFormat.parseContentLength(header);
            } catch (NullPointerException npe) {
                mContentLength = MjpegFormat.getEndOfSeqeunce(this,
                        JpegFormat.EOF_MARKER); //position of first byte after the jpeg
            }

            //(JpegFormat.EOF_MARKER); //position of first byte after the jpeg
        } else {
            //the slow way, because we have to test (if/then) every byte!
            mContentLength = MjpegFormat.getEndOfSeqeunce(this,
                    JpegFormat.EOF_MARKER); //position of first byte AFTER the jpeg
        }

        //create frame array
        byte[] frameData = new byte[headerLen + mContentLength];
        reset();
        readFully(frameData);

        return new MjpegFrame(frameData, mContentLength, mSequence++);
    }

    /**
     * Unit test against an MJPG raw file from the Axis camera.
     *
     * @param args
     */

}