
import javax.sound.midi.*;
import java.io.*;
import java.util.*;

public class DataProcessing {
	public static final int VOLUME = 80;

	boolean[][] input = new boolean[13][16]; //TODO: get input
	// testing code
	
	public MidiEvent toNote(int number, int time){
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.NOTE_ON, 1, 60 + number, VOLUME);
		MidiEvent event = new MidiEvent(msg, time);
		return event;
	}
	
	public MidiEvent toNoteStop(int number, int time){
		ShortMessage msg = new ShortMessage();
		msg.setMessage(ShortMessage.NOTE_OFF, 1, 60 + number, VOLUME);
		MidiEvent event = new MidiEvent(msg,time+1);
		return event;
	}

	public void process() {
		try {
			Sequencer sequencer;
			sequencer = MidiSystem.getSequencer();
			if (sequencer == null){
				System.out.println("midisystem in use");
			} else {
				sequencer.open();
			}
			Sequence sequence = new Sequence(Sequence.PPQ, 16);
			Track track = sequence.createTrack(); // check if this is valid
			for (int i = 0; i < 16; i++) {
				int currentTimeSegment = i;
				for (int j = 0; j < 13; j++) {
					if (input[i][j] == true){
						
						track.add(toNote(i/*note*/,j/*time*/));
						track.add(toNoteStop(i, j));
					}
				}
			}
			int[] allowedTypes = MidiSystem.getMidiFileTypes(sequence);
		    if (allowedTypes.length == 0) {
		        System.err.println("No supported MIDI file types.");
		    } else {
			MidiSystem.write(sequence, allowedTypes[0], new File("file.midi"));
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
}
