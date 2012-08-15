package tcpserver.source;

import javax.sound.sampled.AudioFormat;

public class AudioChunk {
	protected byte data[];
	protected int sampleRate;
	protected int channelsNumber;
	protected int bytesPerSample;
	protected int nextInsertPos;
	protected int numSamples;

	public AudioChunk(final int sampleRate, final int bitsPerSample, final int channelsNumber, final int numSamples) {
		this.sampleRate = sampleRate;
		this.channelsNumber = channelsNumber;
		this.bytesPerSample = bitsPerSample/8;
		this.numSamples = numSamples;
		this.data = new byte[numSamples*channelsNumber*bytesPerSample];
	}

	public AudioChunk(final AudioFormat format, final int numSamples) {
		this.sampleRate = (int) format.getSampleRate();
		this.channelsNumber = (int) format.getChannels();
		this.bytesPerSample = (int) format.getSampleSizeInBits()/8;
		this.numSamples = numSamples;
		this.data = new byte[numSamples*channelsNumber*bytesPerSample];
	}

	public void appendSineWave(int samples, final float frequency[], final int amp) {
		// if amplitude is 0, just fill the samples with 0, without calculating sines
		if (amp == 0) {
			int end = nextInsertPos + samples*channelsNumber*bytesPerSample;
			while (nextInsertPos < end) {
				this.data[nextInsertPos++] = 0;
				if (bytesPerSample > 1) {
					this.data[nextInsertPos++] = 0;
				}
			}
			return;
		}
		
		double w[] = new double[frequency.length]; // array to get the angular frequencies
		final int maxAmplitude = amp / frequency.length; // the maximum amplitude each frequency can have, based on the total maximum amplitude

		// calculate the angular frequency for each frequency
		for (int i = 0; i < w.length; i++) {
			w[i] = (Math.PI * 2 * frequency[i]) / sampleRate;
		}

		// fill the data with the sin waves
		for (int t = 0; t < samples; t++) {
			double floatValue = 0;
			for (double omega: w) {
				floatValue += (maxAmplitude * Math.sin(omega * t));
			}
//			if (floatValue < 0.000000001 && floatValue > -0.000000001)
//				System.out.println(t);
			int value = (int) floatValue;
			
			byte msb = (byte) (value >>> 8);
			byte lsb = (byte) (value);
			
			for (int j = 0; j < channelsNumber; j++) {
				this.data[nextInsertPos++] = msb;
				if (bytesPerSample > 1) {
					this.data[nextInsertPos++] = lsb;
				}
			}
		}
	}
	
	public byte[] getData() {
		return this.data;
	}
	
	public int getLength() {
		return this.numSamples;
	}
	
	public int getLengthInBytes() {
		return numSamples*channelsNumber*bytesPerSample;
	}
	
	public static final byte[] intToByteArray(int value) {
		return new byte[] {
				(byte) (value >>> 24),
				(byte) (value >> 16 & 0xff),
				(byte) (value >> 8 & 0xff),
				(byte) (value & 0xff) };
	}
}
