/*  Skyrim Profile Manager  Copyright (C) 2016  Cameron Van Ravens
 *  This program comes with ABSOLUTELY NO WARRANTY.
 *  This is free software, and you are welcome to redistribute it
 *  under certain conditions; See LICENSE.txt for more details. 
 *  
 *  TODO for future versions:
 *  - Add more character info (race, total play time, last save location)
 *  - Languages implementation (maybe)
 */

package gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


public class Profile extends JFrame implements ItemListener {
	// generated serial UID so there's no need for compiler interpretation
	private static final long serialVersionUID = -6848668798032033955L;

	
	public static String softwareVersion = "1.0.0.0"; // Major, Minor, Maintenance release, build number
	
	
	// Global variables
	public static String selfDir = System.getProperty("user.dir");
	
	public static String steamPath = ""; // /SteamApps/common/Skyrim/SkyrimLauncher.exe -> skyrim app
	public static String skyrimSavesPath = "";
	public static String profilePath = selfDir + "/Profiles/";
	File profileFolder = new File(profilePath);
		
	
	public String currChar = "";
	public String selectedChar = "";
	
	public Vector<String> profileList = new Vector<String>();
	
	
	private static JPanel pnl;
	
	// Header objects
	private JLabel currCharLabel;
	
	// Control objects
	private JComboBox<String> charSelectBox;
	private JButton selectButton;
	private JButton cleanButton;
	private JButton deleteButton;
	private JButton launchButton;
	
	// Info objects
	JTextArea charInfo = new JTextArea();
	
	// Class constructor
	public Profile() {
		// if there are no paths resolved, invoke the first time user setup
		if (new File(selfDir + "/paths.txt").exists()) {
			try {
				steamPath = Files.readAllLines(Paths.get(selfDir + "/paths.txt"),StandardCharsets.UTF_8).get(0);
				skyrimSavesPath = Files.readAllLines(Paths.get(selfDir + "/paths.txt"),StandardCharsets.UTF_8).get(1);
				
			} catch (IOException e) { // delete saves file, restart program.
				JOptionPane.showMessageDialog (null, "Could not open 'paths.txt'. Please restart the program.",
							"Warning: Could not read Paths file",JOptionPane.WARNING_MESSAGE);
				//new File(selfDir + "/paths.txt").delete();
				//steamPath = "";
				//skyrimSavesPath = "";
				System.exit(0);
			}
		} else {
			// Run first time user setup
			getPathSetup("Setup", true);
		}
		
		if (!profileFolder.exists()) { // check if profile folder exists
			profileFolder.mkdirs();
		}
		if (!new File(selfDir + "/DELETED").exists()) { // check if deletion folder exists
			new File(selfDir + "/DELETED").mkdirs();
		}
		
		// Scan current character
 		File saveFolder = new File(skyrimSavesPath);
 		File[] sky_saves = saveFolder.listFiles();
 		for (int i = 0; i < sky_saves.length; i++) {
 			try {
 				String charName = sky_saves[i].getName().split(" ")[3];
 				if (!currChar.equals("") && !currChar.equals(charName)) {
 					currChar = "**Saves Not Initialized**";
 					
 					// invoke the organize saves dialog
 					int dialogResult = JOptionPane.showConfirmDialog (null, "We noticed that your saves are not organized yet! Would you\nlike to do this now? If not, this can be later done\nby navigating to \"Edit -> Organize Saves\" in the menu bar.",
 							"Saves Not Organized Yet",JOptionPane.YES_NO_OPTION);
 					if (dialogResult == JOptionPane.YES_OPTION) {
 						initProfileFolders();
 					}
 					break; // no need to continue checking
 				} else if (currChar.equals("")) {
 					currChar = charName;
 					if (!new File(profilePath + charName).exists()) { // if profile does not have a folder, make it one
 						new File(profilePath + charName).mkdirs();
 					}
 				}
 			} catch (Exception e) {continue;}
 		}
		
 		// scan for profiles
 		profileList = scanProfiles(profilePath);
 		
		initUI();
		
		setTitle("Skyrim Profile Manager");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
	}
	
	private void initUI() {
		pnl = (JPanel) getContentPane(); // for the screen dialogues
		
		setLayout(null);
		
		// Current Character Label
		currCharLabel = new JLabel("Current Character: " + currChar);
		
		
		// Character drop down menu
		charSelectBox = new JComboBox<>(profileList);
		charSelectBox.setSelectedIndex(0);
		charSelectBox.addItemListener(this);
		charSelectBox.setToolTipText("Select the character you wish to select/modify");
		
		// Select Profile button
		selectButton = new JButton("Select Profile");
		selectButton.setEnabled(false);
		selectButton.setToolTipText("Use the profile selected in the game");
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // set selected char to currChar
            	int f = switchCharacter(selectedChar);
            	currCharLabel.setText("Current Character: " + currChar); // reset the header message
            	charInfo.setText("Character switch to " + currChar + " completed. File transfers failed: " + f);
            	if (selectedChar.equals(currChar)) { // if this is the current character
    				selectButton.setEnabled(false);
    				cleanButton.setEnabled(true);
    				deleteButton.setEnabled(true);
            	}
            	repaint();
            }
        });
        
        // Clean Saves button
        cleanButton = new JButton("Clean Saves");
        cleanButton.setEnabled(false);
        cleanButton.setToolTipText("Delete all saves up to but not including your most recent ones");
        cleanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	// invoke dialogue box asking for confirmation, then move saves to recycling bin
            	Object[] options = {"Yes", "Move to bin", "Cancel"};
            	int dialogResponse = JOptionPane.showOptionDialog(null, "Are you sure you wish to clean these saves with permanent deletion?\n(Selecting 'Move to bin' will move the files to a deleted files folder)", "Confirm Save Clean up",
            			JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
            			null, options, options[0]);
            	if (dialogResponse == JOptionPane.YES_OPTION){ // permanently delete saves
	            	int f = cleanSaveFiles(selectedChar, true);
	            	charInfo.setText("Permanent file deletion completed; saves have been cleaned up. File deletions failed: " + f);
            	}
            	else if (dialogResponse == JOptionPane.NO_OPTION) { // move to deleted folder
            		int f = cleanSaveFiles(selectedChar, false);
	            	charInfo.setText("File transfers to DELETED folder completed; saves have been cleaned up. File transfers failed: " + f);
            	}
            } // otherwise do nothing
        });
        
        // Delete Profile button
        deleteButton = new JButton("Delete Profile");
        deleteButton.setEnabled(false);
        deleteButton.setToolTipText("Delete all saves and profile folder of the selected profile");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	// invoke dialogue box asking for confirmation, then move saves to recycling bin
            	Object[] options = {"Yes", "Move to bin", "Cancel"};
            	int dialogResponse = JOptionPane.showOptionDialog(null, "Are you sure you wish to permanently delete this save?\n(Selecting 'Move to bin' will move the files to a deleted files folder)", "Confirm Deletion",
            			JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
            			null, options, options[0]);
            	if (dialogResponse == JOptionPane.YES_OPTION){ // permanently delete saves
	            	int f = deleteCharacter(selectedChar, true);
	            	charSelectBox.setSelectedIndex(0);
	            	charInfo.setText("Permanent file deletion completed. File deletions failed: " + f);
            	}
            	else if (dialogResponse == JOptionPane.NO_OPTION) { // move to deleted folder
            		int f = deleteCharacter(selectedChar, false);
	            	charSelectBox.setSelectedIndex(0);
	            	charInfo.setText("File transfers to DELETED folder completed. File transfers failed: " + f);
            	}
            }
        });
        
        // Launch Skyrim Button
        launchButton = new JButton("Launch Skyrim", new ImageIcon("skyrim.png"));
        launchButton.setHorizontalAlignment(SwingConstants.LEFT);
        if (new File(steamPath + "/SteamApps/common/Skyrim/SkyrimLauncher.exe").exists()) {
        	launchButton.setEnabled(true);
        	launchButton.setToolTipText("Launches the Skyrim app from your Steam folder.");
        } else {
        	launchButton.setEnabled(false);
        	launchButton.setToolTipText("SkyrimLauncher.exe could not be found. Perhaps you gave the wrong location of the Steam folder?");
        }
        
        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	// check if we should show close app message
            	if (new File(selfDir + "/CLOSE_ON_LAUNCH.txt").exists()) { // launch with closing
            		try {
            			new ProcessBuilder(steamPath + "/SteamApps/common/Skyrim/SkyrimLauncher.exe").start(); // start Skyrim
            			System.exit(0); // exit the program
            		} catch (Exception e) {
            			JOptionPane.showMessageDialog(pnl, "Could not launch Skyrim.exe. Perhaps you gave\nthe wrong location of your Steam folder?\nYou can change this going going to Options -> Set Paths.",
                                "Error: Could not launch Skyrim.exe", JOptionPane.ERROR_MESSAGE);
            		}
            	}
            	else if (new File(selfDir + "/DO_NOT_CLOSE_ON_LAUNCH.txt").exists()) { // launch without closing
            		try {
            			new ProcessBuilder(steamPath + "/SteamApps/common/Skyrim/SkyrimLauncher.exe").start(); // start Skyrim
            		} catch (Exception e) {
            			JOptionPane.showMessageDialog(pnl, "Could not launch Skyrim.exe. Perhaps you gave\nthe wrong location of your Steam folder?\nYou can change this going going to Options -> Set Paths.",
                                "Error: Could not launch Skyrim.exe", JOptionPane.ERROR_MESSAGE);
            		}
            	}
            	else { // ask if the program should be closed or not
	            	JCheckBox dontShow=  new JCheckBox("Do not show this message again");
	            	Object[] params = {"Do you wish to close this application when launching Skyrim?", dontShow};
	            	int dialogResponse = JOptionPane.showConfirmDialog(null, params, "Close application on launch?",JOptionPane.YES_NO_OPTION);
	            	
	            	if (dialogResponse == JOptionPane.YES_OPTION) {
	            		if (dontShow.isSelected()) {
	            			try {
	            				PrintWriter writer = new PrintWriter("CLOSE_ON_LAUNCH.txt", "UTF-8");
	            				writer.println("Delete this file to make the dialog appear again.");
	            				writer.close();
	            			} catch (Exception e) {}
	            		}
	            		
	            		try {
	            			new ProcessBuilder(steamPath + "SteamApps/common/Skyrim/SkyrimLauncher.exe").start(); // start Skyrim
	            			System.exit(0); // exit the program
	            		} catch (Exception e) {
	            			JOptionPane.showMessageDialog(pnl, "Could not launch Skyrim.exe. Perhaps you gave\nthe wrong location of your Steam folder?",
	                                "Error: Could not launch Skyrim.exe", JOptionPane.ERROR_MESSAGE);
	            		}
	            	}
	            	else if (dialogResponse == JOptionPane.NO_OPTION) {
	            		if (dontShow.isSelected()) {
	            			try {
	            				PrintWriter writer = new PrintWriter("DO_NOT_CLOSE_ON_LAUNCH.txt", "UTF-8");
	            				writer.println("Delete this file to make the dialog appear again.");
	            				writer.close();
	            			} catch (Exception e) {}
	            		}
	            		
	            		try {
	            			new ProcessBuilder(steamPath + "SteamApps/common/Skyrim/SkyrimLauncher.exe").start(); // start Skyrim
	            		} catch (Exception e) {
	            			JOptionPane.showMessageDialog(pnl, "Could not launch Skyrim.exe. Perhaps you gave\nthe wrong location of your Steam folder?",
	                                "Error: Could not launch Skyrim.exe", JOptionPane.ERROR_MESSAGE);
	            		}
	            	}
            	}
            }
        });
        
        // Character info box
        charInfo.setLineWrap(true);
        charInfo.setEditable(false);
        charInfo.setWrapStyleWord(true);
        charInfo.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(0,5,5,5)));
        charInfo.setText("");
        
        createMenuBar();
        
        // Positioning of elements
        currCharLabel.setBounds(20,10,300,25);
        charSelectBox.setBounds(20,70,120,25);
        selectButton.setBounds(20,115,120,25);
        cleanButton.setBounds(20,160,120,25);
        deleteButton.setBounds(20,205,120,25);
        charInfo.setBounds(170,70,310,160);
        launchButton.setBounds(500 - launchButton.getMaximumSize().width - 20,10,launchButton.getMaximumSize().width,40);
        
        add(currCharLabel);
        add(charSelectBox);
        add(selectButton);
        add(cleanButton);
        add(deleteButton);
        add(charInfo);
        add(launchButton);
    }
	
	private void createMenuBar() {

        JMenuBar menubar = new JMenuBar();

        // File Menu
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);

        JMenuItem f_open = new JMenuItem("Open Profile Location", UIManager.getIcon("FileChooser.upFolderIcon"));
        f_open.setMnemonic(KeyEvent.VK_P);
        f_open.setToolTipText("Opens the folder which stores the profile files");
        f_open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // open the folder containing the profile folders
            	try {
            		Desktop.getDesktop().open(profileFolder); // apparently this works for OSX too...
            	} catch(Exception e) {
            		charInfo.setText("Error: Could not open profile path. Perhaps you moved the folders or haven't initialized your saves yet?");
            	}
            }
        });
        file.add(f_open);
        
        JMenuItem f_openD = new JMenuItem("Open Deletion Location", UIManager.getIcon("FileChooser.upFolderIcon"));
        f_openD.setMnemonic(KeyEvent.VK_D);
        f_openD.setToolTipText("Opens the folder which stores the deleted files");
        f_openD.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // open the folder containing the profile folders
            	try {
            		Desktop.getDesktop().open(new File(selfDir + "/DELETED")); // apparently this works for OSX too...
            	} catch(Exception e) {
            		charInfo.setText("Could not open deleted folder path. Perhaps you moved the folder?");
            	}
            }
        });
        file.add(f_openD);
        
        JMenuItem f_exit = new JMenuItem("Exit");
        f_exit.setMnemonic(KeyEvent.VK_X);
        f_exit.setToolTipText("Exit application");
        f_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	System.exit(0); // close the program
            }
        });
        file.add(f_exit);
        
        menubar.add(file);

        // Edit Menu
        JMenu edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        
        JMenuItem e_setup = new JMenuItem("Organize saves", UIManager.getIcon("FileView.hardDriveIcon"));
        e_setup.setMnemonic(KeyEvent.VK_G);
        e_setup.setToolTipText("Puts the unorganized saves into individual save folders");
        e_setup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int f = initProfileFolders();
    			selectedChar = profileList.get(0);
    			charSelectBox.setSelectedIndex(0);
    			charInfo.setText("Save file organization completed. Transfers Failed: " + f);
    			
            }
        });
        
        edit.add(e_setup);
        
        JMenuItem e_newProfile = new JMenuItem("Create New Profile", UIManager.getIcon("FileChooser.newFolderIcon"));
        e_newProfile.setMnemonic(KeyEvent.VK_N);
        e_newProfile.setToolTipText("Create a new profile folder and prepare the game's save folder.");
        e_newProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String dialogResult = JOptionPane.showInputDialog(pnl, "Enter Character Name: ", "New Character Setup", JOptionPane.PLAIN_MESSAGE);
                
                if (!dialogResult.isEmpty()) {
                	if (!new File(profilePath + dialogResult).mkdirs()) {
                		charInfo.setText("Could not make new profile.");
                	} else {
                		charInfo.setText("Profile created successfully.");
                		profileList.add(dialogResult);
                		charSelectBox.setSelectedItem(dialogResult);
                	}
                }
            }
        });
        
        edit.add(e_newProfile);        
        
        menubar.add(edit);
        
        // Options Menu
        JMenu options = new JMenu("Options");
        options.setMnemonic(KeyEvent.VK_O);
        
        JMenuItem o_setPaths = new JMenuItem("Set Paths", UIManager.getIcon("FileChooser.detailsViewIcon"));
        o_setPaths.setMnemonic(KeyEvent.VK_S);
        o_setPaths.addActionListener(new ActionListener() { // open the setup paths dialog
            @Override
            public void actionPerformed(ActionEvent event) {
            	getPathSetup("Set Paths", false);
            	if (new File(steamPath + "/SteamApps/common/Skyrim/SkyrimLauncher.exe").exists()) {
                	launchButton.setEnabled(true);
                	launchButton.setToolTipText("Launches the Skyrim app from your Steam folder.");
                } else {
                	launchButton.setEnabled(false);
                	launchButton.setToolTipText("Skyrim.exe could not be found. Perhaps you gave the wrong location of the Steam folder?");
                }
            	repaint();
            }
        });
        
        options.add(o_setPaths);
        
        menubar.add(options);
        
        // Help Menu
        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        
        JMenuItem h_help = new JMenuItem("About", new ImageIcon("about.gif"));
        h_help.setMnemonic(KeyEvent.VK_A);
        h_help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                help();
            }
        });
        
        help.add(h_help);
        
        menubar.add(help);
        
        setJMenuBar(menubar);
    }

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException,UnsupportedLookAndFeelException {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Profile ex = new Profile();
                ex.setVisible(true);
            }
        });
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	        public void run() {
	            try {
					writeOnExit();
				} catch (IOException e) { // show error message if we fail to write into the save file
					JOptionPane.showMessageDialog(pnl, "Could not write path information to file.","Error: Could not write to file.", JOptionPane.WARNING_MESSAGE);
				}
	        }
	    }, "Shutdown-thread"));
	}
	
	// Handles the change event of the profile combo box
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			selectedChar = e.getItem().toString();
			if (selectedChar.equals(profileList.get(0))) { // make all three buttons not clickable
				selectButton.setEnabled(false);
				cleanButton.setEnabled(false);
				deleteButton.setEnabled(false);
			}
			else if (selectedChar.equals(currChar)) { // if this is the current character
				selectButton.setEnabled(false);
				cleanButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
			else { // any other character
				selectButton.setEnabled(true);
				cleanButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
			
			if (selectedChar.equals(profileList.get(0))) { // updating the character info
				charInfo.setText("");
			} else {
				String date = "";
				try {
					date = getLastSaveDate(selectedChar);
				} catch (Exception e1) {}
				
				charInfo.setText("Name: " + selectedChar + "\nLast Save Date: " + (date == "" ? "No recents saves" : date));
			}
        }
	}
	
	
	/** METHODS **/
	// Scans the profile folder to update the list
	public Vector<String> scanProfiles(String path) {
		Vector<String> list = new Vector<String>();
		list.add("- Select Profile -"); // adding default option
		
		File profileFolder = new File(path);
		String[] profiles = profileFolder.list();
		
		for (int i = 0; i < profiles.length; i++) {
			if (list.indexOf(profiles[i]) == -1) {
				list.add(profiles[i]);
			}
		}
		
		return list;
	}

	// getting the last save date of the most recent save
	public String getLastSaveDate(String charName) throws IOException {
		File lastSave = new File(""); // initializing variable

		// getting the latest save file
		if (charName.equals(currChar)) { // grab save file from main save folder
			File saveFolder = new File(skyrimSavesPath);
			File[] listOfSaves = saveFolder.listFiles();
			lastSave = listOfSaves[listOfSaves.length - 1];
		} else { // if not in the main save file, check save folder...
			File saveFolder = new File(profilePath + charName);
			File[] listOfSaves = saveFolder.listFiles();
			lastSave = listOfSaves[listOfSaves.length - 1];
		}

		return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(lastSave.lastModified());
	}
	
	// Handles selecting the character
	public int switchCharacter(String charName) {
		// moving current character back to their profile folder
		int failCount = 0;
		File saveFolder = new File(skyrimSavesPath);
		File[] listOfSaves = saveFolder.listFiles();
		
		if (!currChar.equals("")) {
			for (int i = 0; i < listOfSaves.length; i++) {
				try {
					if (listOfSaves[i].getName().split(" ")[3].equals(currChar)) {
						if (!listOfSaves[i].renameTo(new File(profilePath + currChar + "/" + listOfSaves[i].getName()))) // puts in folder with char name
							failCount++;
					}
				} catch(Exception e) {}// error on file transfer
			}
		}
		
		// Move new character to Skyrim's save folder
		currChar = charName; // setting currChar to the new character selected
		File profileF = new File(profilePath + "/" + currChar);
		File[] profileSaves = profileF.listFiles();
		
		for (int i = 0; i < profileSaves.length; i++) {
			try {
				if (!profileSaves[i].renameTo(new File(skyrimSavesPath + "/" + profileSaves[i].getName()))) // move save to save folder
					failCount++;
			} catch (Exception e) {} // catching failures
		}
		return failCount;
	}

	// Handle cleaning up the save files
	public int cleanSaveFiles(String charName, boolean delFiles) {
		Vector<File> saveFilesToFilter = new Vector<File>();
		int failCount = 0;
		
		if (charName.equals(currChar)) { // have to work in game save folder then
			File saveFolder = new File(skyrimSavesPath);
			File[] listOfSaves = saveFolder.listFiles();
			
			for (int i = 0; i < listOfSaves.length; i++) {
				try {
					if (listOfSaves[i].getName().split(" ")[3].equals(charName)) {
						saveFilesToFilter.add(listOfSaves[i]); // add file to list to process
					}
				} catch (Exception e) {} // we just need to catch out-of-index errors
			}
		} else { // otherwise it's in the profile directory
			File saveFolder = new File(profilePath + charName);
			saveFilesToFilter = new Vector<File>(Arrays.asList(saveFolder.listFiles()));
		}
		
		String lastSaveNum = "";
		if (saveFilesToFilter.size() > 0)
			lastSaveNum = saveFilesToFilter.lastElement().getName().split(" ")[1];
		
		for (int i = 0; i < saveFilesToFilter.size(); i++) {
			if (delFiles) {
				if (!saveFilesToFilter.get(i).getName().split(" ")[1].equals(lastSaveNum)) {
					if (!saveFilesToFilter.get(i).delete()) // delete file permanently
						failCount++;
				}
			} else {
				if (!saveFilesToFilter.get(i).getName().split(" ")[1].equals(lastSaveNum)) {
					if (!saveFilesToFilter.get(i).renameTo(new File(selfDir + "/DELETED/" + saveFilesToFilter.get(i).getName()))) // move to deleted folder
						failCount++;
				}
			}
		}
		
		return failCount;
	}
	
	// Handles moving characters to the recycling bin
	public int deleteCharacter(String charName, boolean delFiles) {
		int failCount = 0;
		
		if (charName.equals(currChar)) { // if so, then we have to delete them from the save folder first
			currChar = "";
			
			File saveFolder = new File(skyrimSavesPath);
			File[] listOfFiles = saveFolder.listFiles();
			
			for (int i = 0; i < listOfFiles.length; i++) {
				try {
					if (listOfFiles[i].getName().split(" ")[3].equals(charName)) { // apparently there no programmable path for the recycle bin
						if (delFiles) { // deletes the file permanently
							if (!listOfFiles[i].delete())
								failCount++;
						} else { // move to the DELETED folder
							if (!listOfFiles[i].renameTo(new File(selfDir + "/DELETED/" + listOfFiles[i].getName()))) // move file to DELETED folder
								failCount++;
						}
					}
				} catch (Exception e) {failCount++;}
			}
		} else { // otherwise just delete the folder
			File toBeDeleted = new File(profilePath + charName);
			File[] listOfFiles = toBeDeleted.listFiles();
			
			for (int i = 0; i < listOfFiles.length; i++) {
				try {
					if (delFiles) { // deletes the file permanently
						if (!listOfFiles[i].delete())
							failCount++;
					} else { // move to the DELETED folder
						if (!listOfFiles[i].renameTo(new File(selfDir + "/DELETED/" + listOfFiles[i].getName()))) // move file to DELETED folder
							failCount++;
					}
				} catch (Exception e) {failCount++;}
			}
			
			toBeDeleted.delete(); // delete the character's profile folder
		}
		
		if (failCount == 0)
			profileList.remove(charName); // only remove if there wasn't any errors
		
		return failCount;
	}
	
	// puts the unorganized saves into individual save folders
	public int initProfileFolders() {
		int failCount = 0;
		File saveFolder = new File(skyrimSavesPath);
		File[] listOfSaves = saveFolder.listFiles();
		
		// Getting character names and setting up locations
		ArrayList<String> charList = new ArrayList<String>(); // arrayList since we do not know how many saves there are
		for (int i = 0; i < listOfSaves.length; i++) {
			// saves are of format: <save#> - <charName> <totalPlayTime>
			try {
				String charName = listOfSaves[i].getName().split(" ")[3]; // get the character name
				if (charList.indexOf(charName) == -1) { // is not in the list already
					charList.add(charName); // store name in next empty position
				}
			} catch(Exception e) {continue;} // skipping over weird files (might need to control this cause of autosaves)
		}
		
		// moving the saves to their appropriate save folder
		ArrayList<File> filesToTransfer; // arrayList of files to be moved
		
		for (int i = 0; i < charList.size(); i++) {
			listOfSaves = saveFolder.listFiles(); // updating the file list
			filesToTransfer = new ArrayList<File>(); // refreshing the transfer list
			
			for (int j = 0; j < listOfSaves.length; j++) {
				// saves are of format: <save#> - <charName> <totalPlayTime>
				try {
					String charName = listOfSaves[j].getName().split(" ")[3]; // get the character name
					if (charName.equals(charList.get(i))) { // if this is the name we're looking for
						filesToTransfer.add(listOfSaves[j]); // add file to be transfered
					}
				} catch(Exception e) {continue;} // skipping over weird files (might need to control this cause of autosaves)
			}
			
			// updating profileList
			if (profileList.indexOf(charList.get(i)) == -1) {
				profileList.add(charList.get(i));
			}
			
			// move files to profile folder
			new File(profilePath + charList.get(i)).mkdir(); // make the directory first
			for (int j = 0; j < filesToTransfer.size(); j++) {
				try {
					if (!filesToTransfer.get(j).renameTo(new File(profilePath + charList.get(i) + "/" + filesToTransfer.get(j).getName()))) // puts in folder with char name
						failCount++;
				} catch(Exception e) {failCount++;} // error on file transfer
			}
		}
		if (failCount == 0) {
			currChar = "";
			currCharLabel.setText("Current Character: " + currChar);
		}
		return failCount; // returns number of files that could not be moved
	}

	// Contains the formatting and functionality of the first time setup
	public void getPathSetup(String dialogTitle, boolean termOnCancel) {
		// Layout of the dialog
		JPanel panel = new JPanel(new BorderLayout());
		
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		// Steam path
		JPanel steamArea = new JPanel(new FlowLayout());
		
		steamArea.add(new JLabel("Steam Path:"));
		JTextField steam_path = new JTextField("C:/Program Files (x86)/Steam");
		steam_path.setEditable(false);
		steam_path.setPreferredSize(new Dimension(200,25));
		steamArea.add(steam_path);
		
		JButton steamSelectFile = new JButton();
		steamSelectFile.setIcon(UIManager.getIcon("FileView.directoryIcon"));
		steamSelectFile.setToolTipText("Select location of Steam directory");
		steamSelectFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	chooser.setDialogTitle("Choose Steam Directory");
            	chooser.setCurrentDirectory(new File("C:/Program Files (x86)/Steam"));
                int result = chooser.showDialog(panel, "Select Directory");
                if (result == JFileChooser.APPROVE_OPTION) {
                	steam_path.setText(chooser.getSelectedFile().getName()); // set chosen path to textbox
                }
            }
        });
		steamArea.add(steamSelectFile);
		
		// Save path
		JPanel saveArea = new JPanel(new FlowLayout());
		
		saveArea.add(new JLabel("Skyrim Save Folder Path:"));
		JTextField save_path = new JTextField("C:/Users/" + System.getProperty("user.name") + "/Documents/My Games/Skyrim/Saves");
		save_path.setEditable(false);
		save_path.setPreferredSize(new Dimension(200,25));
		saveArea.add(save_path);
		
		JButton saveSelectFile = new JButton();
		saveSelectFile.setIcon(UIManager.getIcon("FileView.directoryIcon"));
		saveSelectFile.setToolTipText("Select location of Skyrim game save directory");
		saveSelectFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
            	chooser.setDialogTitle("Choose Skyrim Save Directory");
            	chooser.setCurrentDirectory(new File("C:/Users/Cameron/Documents/My Games/Skyrim/Saves"));
                int result = chooser.showDialog(panel, "Select Directory");
                if (result == JFileChooser.APPROVE_OPTION) {
                	save_path.setText(chooser.getSelectedFile().getName()); // set chosen path to textbox
                }
            }
        });
		saveArea.add(saveSelectFile);
		
		panel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
		panel.add(steamArea, BorderLayout.EAST);
		panel.add(saveArea, BorderLayout.SOUTH);
		
		int setupDialog = JOptionPane.showConfirmDialog(null, panel, dialogTitle,
	            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (setupDialog == JOptionPane.OK_OPTION) {
			// set variables
			steamPath = steam_path.getText();
			skyrimSavesPath = save_path.getText();
		} else {
			if (termOnCancel)
				System.exit(0); // close the program otherwise
		}
	}
	
	// Runs this code on program termination
	public static void writeOnExit() throws IOException {
		// write variables to text file
		if (!steamPath.equals("") && !skyrimSavesPath.equals("")) {
			PrintWriter writer = new PrintWriter(selfDir + "/paths.txt", "UTF-8");
			writer.println(steamPath);
			writer.println(skyrimSavesPath);
			writer.close();
		}
	}

	/* Information Method -- Help */
	public void help() {
		String text = "Version: " + softwareVersion + "<br>Author: Cameron Van Ravens<br>Licensing: <a href=\"http://www.gnu.org/licenses/\">GNU GPL-3.0</a><br><br>"
				+ "How do I use this program?<br><br>This program is essentially an automated form of manually switching around"
				+ " sets of save files. This program allows you to do this with just a single click, saving you all the copy and"
				+ " pasting and the time spent navigating your hard drive. It also allows you to utilize some other functions"
				+ " such as deleting saves, cleaning up saves, and creating new profile folders allowing you to start with a fresh"
				+ " saves slate for starting a new game. Eventually other functionalities such as individual profile mod loading"
				+ " will be available. For now, we will just work with the basics of what we need to effectively switch profiles"
				+ " in Skyrim.<br><br>"
				+ "Firstly, the program should have walked you through the process of getting everything setup, as well as"
				+ " getting you to initialize your profile folders for the first time. From there, to choose a character to play"
				+ " just select the character from the drop down menu, then click the \"Select Character\" button that should be"
				+ " right below the drop down menu. From there you can then click the \"Launch Skyrim\" button and you're all ready"
				+ " to go. Now, you can also use the other functions such as \"Clean up Saves\" and \"Delete Save\" without selecting"
				+ " the character. Whoever you have selected in the drop down menu is the character that you are applying those functions"
				+ " to.<br><br>Disclaimer:<br><br>Skyrim Profile Manager  Copyright (C) 2016  Cameron Van Ravens. This program comes with ABSOLUTELY"
				+ " NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; See LICENSE.txt for details.<br><br>"
				+ " Cameron Van Ravens<br>cvr@cogeco.ca<br><a href=\"https://github.com/camvr\">https://github.com/camvr</a>";
		
		JEditorPane msg = new JTextPane();
		msg.setContentType("text/html");
		msg.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		msg.setEditable(false);
		msg.setText(text);
		msg.addHyperlinkListener(new HyperlinkListener() {
		    public void hyperlinkUpdate(HyperlinkEvent e) {
		        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		        	if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(pnl, "Could not open hyperlink.", "Error", JOptionPane.WARNING_MESSAGE);
						}
		        	}
		        }
		    }
		});
		msg.setCaretPosition(0);
		
		JScrollPane scrollPane = new JScrollPane(msg);
		scrollPane.setPreferredSize(new Dimension(400,200));
		JOptionPane.showMessageDialog(null, scrollPane, "About", JOptionPane.PLAIN_MESSAGE);
	}
}
