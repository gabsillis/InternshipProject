
import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;

import java.io.*;
import java.util.*;

public class DataProcessing {
	public static final int VOLUME = 80;

	boolean[][] input = new boolean[13][16]; //TODO: get input
	// testing code
	
	
	public MidiEvent toNote(int number, int time){
		ShortMessage msg = new ShortMessage();
		try {
			msg.setMessage(ShortMessage.NOTE_ON, 1, 60 + number, VOLUME);
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MidiEvent event = new MidiEvent(msg, time);
		return event;
	}
	
	public MidiEvent toNoteStop(int number, int time){
		ShortMessage msg = new ShortMessage();
		try {
			msg.setMessage(ShortMessage.NOTE_OFF, 1, 60 + number, VOLUME);
		} catch (InvalidMidiDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		MidiEvent event = new MidiEvent(msg,time+1);
		return event;
	}

	public void process() {
		// test code
		input[3][2] = true;
		input[5][7] = true;
		input[8][3] = true;
		// end test code
		try {
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			MidiDevice device = null;
			for (int i = 0; i< infos.length; i++){
				device = MidiSystem.getMidiDevice(infos[i]);
			
				if(device instanceof Sequencer){
					System.out.println(device.getDeviceInfo());
					break;
				}
			}
			if (device == null){
				System.out.println("midisystem in use");
			} else {
				device.open();
			}
			Sequencer sequencer;
			//might not need a sequencer
			Sequence sequence = new Sequence(Sequence.PPQ,1);
			
			Track track = sequence.createTrack(); // check if this is valid
			for (int i = 0; i < 13; i++) {
				System.out.println(i);
				int currentTimeSegment = i;
				for (int j = 0; j < 16; j++) {
					if (input[i][j] == true){
						
						track.add(toNote(i/*note*/,j/*time*/));
						if(i<16 && input[i+1][j] != true){
							track.add(toNoteStop(i, j));
						}
					}
				}
			}
			int[] allowedTypes = MidiSystem.getMidiFileTypes(sequence);
		    if (allowedTypes.length == 0) {
		        System.err.println("No supported MIDI file types.");
		    } else {
		    	String tempDir = System.getProperty("user.home") + "/midirecord";
				File tmpDir = new File(tempDir);
				if(!tmpDir.exists()){
					tmpDir.mkdir();
				}
				if (tmpDir.isFile()){
					System.err.println(tmpDir + " is a file");
					System.exit(1);
				}
				MidiSystem.write(sequence, allowedTypes[0], new File(tempDir,"file.midi"));
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

