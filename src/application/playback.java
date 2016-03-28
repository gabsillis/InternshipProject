package application;

import java.io.File;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Transmitter;

public class playback {
	

	public void play() {
		try{
			Transmitter transmitter;
			Receiver receiver;
			Sequencer sequencer = MidiSystem.getSequencer();
			sequencer.open();
			@SuppressWarnings("unused")
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
			
			String tempDir =  "C:/midirecord";
			receiver = device.getReceiver();
			transmitter = sequencer.getTransmitter();
			transmitter.setReceiver(receiver);
			Sequence sequence = MidiSystem.getSequence(new File(tempDir,"file.midi"));
			
			
			
			
			sequencer.setSequence(sequence);
			
			sequencer.start();
			Thread.sleep(5000);
			sequencer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}
	

}
