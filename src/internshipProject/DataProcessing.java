package internshipProject;

import javax.sound.midi.*;

import java.io.*;
import java.util.*;

public class DataProcessing {

	boolean[][] input = new boolean[13][16];

	public void process() {
		try {
			Sequencer sequencer;
			sequencer = MidiSystem.getSequencer();
			if (sequencer == null){
				System.out.println("midisystem in use");
			} else {
				sequencer.open();
			}
			// each note will get its own track
			Track[] track = new Track[13];
			Sequence sequence = new Sequence(Sequence.PPQ, 16);
			for (int i = 0; i < 16; i++) {
				int currentTimeSegment = i;
				for (int j = 0; j < 13; j++) {
					
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
