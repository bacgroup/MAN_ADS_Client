/* KeyCode_FileBased.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:37 $
 *
 * Copyright (c) 2005 Propero Limited
 * 
 * Author: David LECHEVALIER <david@ulteo.com> 2011
 * Copyright (c) 2011 Ulteo SAS
 *
 * Purpose: Read and supply keymapping information
 *          from a file
 */
package net.propero.rdp.keymapping;

import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import net.propero.rdp.Input;
import net.propero.rdp.Options;

import org.apache.log4j.Logger;
import org.ulteo.utils.IniFile;

public abstract class KeyCode_FileBased {
	public static final String KEYCODE_SECTION = "keycode";
	public static final String GLOBAL_SECTION = "global";
	public static final String SHIFTCAPSLOCK_SECTION = "shiftcapslock";
	public static final String CAPSLOCK_SECTION = "capslock";
	public static final String SHIFT_SECTION = "shift";
	public static final String NOSHIFT_SECTION = "noshift";
	public static final String ALTGR_SECTION = "altgr";

    private Hashtable keysCurrentlyDown = new Hashtable();

    private KeyEvent lastKeyEvent = null;

    private boolean lastEventMatched = false;

    protected static Logger logger = Logger.getLogger(Input.class);

    public static final int SCANCODE_EXTENDED = 0x80;

    public static final int DOWN = 1;

    public static final int UP = 0;

    public static final int QUIETUP = 2;

    public static final int QUIETDOWN = 3;

    private int mapCode = -1;

    private boolean altQuiet = false;

    public boolean useLockingKeyState = true;

    public boolean capsLockDown = false;
    public IniFile binding = null;

    Vector keyMap = new Vector();
    
    private Options opt = null;

    private void updateCapsLock(KeyEvent e) {

    }
    
    public KeyCode_FileBased(InputStream fstream, Options opt_) throws KeyMapException{
        this.opt = opt_;
        readMapFile(fstream);
    }

    /**
     * Constructor for a keymap generated from a specified file, formatted in
     * the manner of a file generated by the writeToFile method
     * 
     * @param keyMapFile
     *            File containing keymap data
     */
    public KeyCode_FileBased(String keyMapFile, Options opt_) throws KeyMapException {
        // logger.info("String called keycode reader");
        int lineNum = 0; // current line number being parsed
        String line = ""; // contents of line being parsed

        boolean mapCodeSet = false;
        this.opt = opt_;
        
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(keyMapFile);
            readMapFile(fstream);
        } catch (FileNotFoundException e) {
            throw new KeyMapException("KeyMap file not found: " + keyMapFile);
        }
    }

    /**
     * Read in a keymap definition file and add mappings to internal keymap
     * @param fstream Stream connected to keymap file
     * @throws KeyMapException
     */
    public void readMapFile(InputStream fstream) throws KeyMapException {
        // logger.info("Stream-based keycode reader");
		String mapCodeString = null;
		this.binding = new IniFile();
		this.binding.load(fstream);
		
		if (this.binding == null)
			throw new KeyMapException("Keymap file is empty");

		mapCodeString = this.binding.getKeyValue(GLOBAL_SECTION, "code");
		try {
			this.mapCode = (int)Long.parseLong(mapCodeString.replace("0x", ""), 16);
		}
		catch (NumberFormatException e) {
			throw new KeyMapException("Unable to get keymap code: "+mapCodeString);
		}
    }
    
    public int getSpecialKey(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_BACK_SPACE:
			return 0xe;
		case KeyEvent.VK_TAB:
			return 0xf;
		case KeyEvent.VK_ENTER:
			return 0x1c;
		case KeyEvent.VK_KP_DOWN:
		case KeyEvent.VK_DOWN:
			return 0x50 | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_KP_LEFT:
		case KeyEvent.VK_LEFT:	
			return 0x4b | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_KP_RIGHT:
		case KeyEvent.VK_RIGHT:	
			return 0x4d | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_KP_UP:
		case KeyEvent.VK_UP:
			return SCANCODE_EXTENDED|0x48;
		case KeyEvent.VK_ESCAPE:
			return 0x1;
		case KeyEvent.VK_PAGE_DOWN:
			return 0x51 | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_PAGE_UP:
			return 0x49 | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_END:
			return 0x4f | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_HOME:
			return 0x47 | KeyCode.SCANCODE_EXTENDED;
		case KeyEvent.VK_INSERT:
			return SCANCODE_EXTENDED | 0x52;
		case KeyEvent.VK_F1:
			return 0x3b;
		case KeyEvent.VK_F2:
			return 0x3c;
		case KeyEvent.VK_F3:
			return 0x3d;
		case KeyEvent.VK_F4:
			return 0x3e;
		case KeyEvent.VK_F5:
			return 0x3f;
		case KeyEvent.VK_F6:
			return 0x40;
		case KeyEvent.VK_F7:
			return 0x41;
		case KeyEvent.VK_F8:
			return 0x42;
		case KeyEvent.VK_F9:
			return 0x43;
		case KeyEvent.VK_F10:
			return 0x44;
		case KeyEvent.VK_F11:
			return 0x57;
		case KeyEvent.VK_F12:
			return 0x58;
		case KeyEvent.VK_DELETE:
			return  SCANCODE_EXTENDED | 0x53;
		case KeyEvent.VK_PROPS:
		case KeyEvent.VK_CONTEXT_MENU:	
			return SCANCODE_EXTENDED | 0xdd;
		case KeyEvent.VK_NUMPAD0:
			return 0x52;
		case KeyEvent.VK_NUMPAD1:
			return 0x4f;
		case KeyEvent.VK_NUMPAD2:
			return 0x50;
		case KeyEvent.VK_NUMPAD3:
			return 0x51;
		case KeyEvent.VK_NUMPAD4:
			return 0x4b;
		case KeyEvent.VK_NUMPAD5:
			return 0x4c;
		case KeyEvent.VK_NUMPAD6:
			return 0x4d;
		case KeyEvent.VK_NUMPAD7:
			return 0x47;
		case KeyEvent.VK_NUMPAD8:
			return 0x48;
		case KeyEvent.VK_NUMPAD9:
			return 0x49;
		case KeyEvent.VK_MULTIPLY:
			return 0x37;
		case KeyEvent.VK_ADD:
			return 0x4e;
		case KeyEvent.VK_SUBTRACT:
			return 0x4a;
		case KeyEvent.VK_DECIMAL:
			return 0x53;
		case KeyEvent.VK_DIVIDE:
			return 0x35 | KeyCode.SCANCODE_EXTENDED;
		default:
			return -1;
		}
	}
    
	public int getFromMap(String section, String key) {
		return this.binding.getKeyIntValue(section, key, -1);
	}
    
    /**
     * Get the RDP code specifying the key map in use
     * @return ID for current key map
     */
    public int getMapCode() {
        return mapCode;
    }

    /**
     * Construct a list of changes to key states in order to correctly send the
     * key action jointly defined by the supplied key event and mapping
     * definition.
     * 
     * @param e
     *            Key event received by Java (defining current state)
     * @param theDef
     *            Key mapping to define desired keypress on server end
     */
    public String stateChanges(KeyEvent e, MapDef theDef) {

        String changes = "";

        final int SHIFT = 0;
        final int CTRL = 1;
        final int ALT = 2;
        final int CAPSLOCK = 3;

        int BEFORE = 0;
        int AFTER = 1;

        boolean[][] state = new boolean[4][2];

        state[SHIFT][BEFORE] = e.isShiftDown();
        state[SHIFT][AFTER] = theDef.isShiftDown();

        state[CTRL][BEFORE] = e.isControlDown();
        state[CTRL][AFTER] = theDef.isCtrlDown();

        state[ALT][BEFORE] = e.isAltDown() ;
        state[ALT][AFTER] = theDef.isAltDown();

        updateCapsLock(e);

        state[CAPSLOCK][BEFORE] = capsLockDown;
        state[CAPSLOCK][AFTER] = theDef.isCapslockOn();

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            AFTER = 0;
            BEFORE = 1;
        }

        if ((e == null) || (theDef == null) || (!theDef.isCharacterDef()))
            return "";

        String up = "" + ((char) UP);
        String down = "" + ((char) DOWN);
        String quietup = up;
        String quietdown = down;

        quietup = "" + ((char) QUIETUP);
        quietdown = "" + ((char) QUIETDOWN);

        if (state[SHIFT][BEFORE] != state[SHIFT][AFTER]) {
            if (state[SHIFT][BEFORE])
                changes += ((char) 0x2a) + up;
            else
                changes += ((char) 0x2a) + down;
        }

        if (state[CAPSLOCK][BEFORE] != state[CAPSLOCK][AFTER]) {
            changes += ((char) 0x3a) + down + ((char) 0x3a) + up;
        }

        return changes;
    }

    /**
     * Output key map definitions to a file as a series of single line text
     * descriptions
     * 
     * @param filename
     *            File in which to store definitions
     */
    public void writeToFile(String filename) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            PrintStream p = new PrintStream(out);

            Iterator i = keyMap.iterator();

            while (i.hasNext()) {
                ((MapDef) i.next()).writeToStream(p);
            }

            p.close();

        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     * 
     * @param c
     *            Character to obtain scancode for
     * @param mod
     *            List of modifiers to be updated by method
     * @return Scancode of supplied key
     */
    public boolean hasScancode(char c) {
        if (c == KeyEvent.CHAR_UNDEFINED)
            return false;

        Iterator i = keyMap.iterator();
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        return (best != null);

    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     * 
     * @param c
     *            Character to obtain scancode for
     * @param mod
     *            List of modifiers to be updated by method
     * @return Scancode of supplied key
     */
    public int charToScancode(char c, String[] mod) {
        Iterator i = keyMap.iterator();
        int smallestDist = -1;
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        if (best != null) {
            if (best.isShiftDown())
                mod[0] = "SHIFT";
            else if (best.isCtrlDown() && best.isAltDown())
                mod[0] = "ALTGR";
            else
                mod[0] = "NONE";
            return best.getScancode();
        } else
            return -1;

    }

    /**
     * Return a mapping definition associated with the supplied key event from
     * within the list stored in this object.
     * 
     * @param e
     *            Key event to retrieve a definition for
     * @return Mapping definition for supplied keypress
     */
    public MapDef getDef(KeyEvent e) {

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            MapDef def = (MapDef) keysCurrentlyDown.get(new Integer(e
                    .getKeyCode()));
            registerKeyEvent(e, def);
            if (e.getID() == KeyEvent.KEY_RELEASED)
                logger.debug("Released: " + e.getKeyCode()
                        + " returned scancode: "
                        + ((def != null) ? "" + def.getScancode() : "null"));
            return def;
        }

        updateCapsLock(e);

        Iterator i = keyMap.iterator();
        int smallestDist = -1;
        MapDef best = null;

        boolean noScanCode = !hasScancode(e.getKeyChar());

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            boolean applies;

            if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                applies = current.appliesToPressed(e);
            } else if ((!lastEventMatched) && (e.getID() == KeyEvent.KEY_TYPED)) {
                applies = current.appliesToTyped(e, capsLockDown);
            } else
                applies = false;

            if (applies) {
                int d = current.modifierDistance(e, capsLockDown);
                if ((smallestDist == -1) || (d < smallestDist)) {
                    smallestDist = d;
                    best = current;
                }
            }
        }

        if (e.getID() == KeyEvent.KEY_PRESSED)
            logger.debug("Pressed: " + e.getKeyCode() + " returned scancode: "
                    + ((best != null) ? "" + best.getScancode() : "null"));
        if (e.getID() == KeyEvent.KEY_TYPED)
            logger.debug("Typed: " + e.getKeyChar() + " returned scancode: "
                    + ((best != null) ? "" + best.getScancode() : "null"));

        registerKeyEvent(e, best);

        return best;
    }

    /**
     * Return a scancode for the supplied key event, from within the mapping
     * definitions stored in this object.
     * 
     * @param e
     *            Key event for which to determine a scancode
     * @return Scancode for the supplied keypress, according to current mappings
     */
    public int getScancode(KeyEvent e) {
        String[] mod = { "" };

        MapDef d = getDef(e);

        if (d != null) {
            return d.getScancode();
        } else
            return -1;
    }

    private void registerKeyEvent(KeyEvent e, MapDef m) {

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            keysCurrentlyDown.remove(new Integer(e.getKeyCode()));
            if ((!this.opt.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Turning CAPSLOCK off - key release");
                capsLockDown = false;
            }
            lastEventMatched = false;
        }

        if (e.getID() == KeyEvent.KEY_PRESSED) {
            lastKeyEvent = e;
            if (m != null)
                lastEventMatched = true;
            else
                lastEventMatched = false;
            if ((this.opt.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Toggling CAPSLOCK");
                capsLockDown = !capsLockDown;
            } else if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
                logger.debug("Turning CAPSLOCK on - key press");
                capsLockDown = true;
            }
        }

        if (lastKeyEvent != null
                && m != null
                && !keysCurrentlyDown.containsKey(new Integer(lastKeyEvent
                        .getKeyCode()))) {
            keysCurrentlyDown.put(new Integer(lastKeyEvent.getKeyCode()), m);
            lastKeyEvent = null;
        }

    }

    /**
     * Construct a list of keystrokes needed to reproduce an AWT key event via RDP
     * @param e Keyboard event to reproduce
     * @return List of character pairs representing scancodes and key actions to send to server
     */
    public String getKeyStrokes(KeyEvent e) {
        String codes = "";
        MapDef d = getDef(e);

        if (d == null)
            return "";

        codes = stateChanges(e, d);

        String type = "";

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            if ((!this.opt.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Sending CAPSLOCK toggle");
                codes = "" + ((char) 0x3a) + ((char) DOWN) + ((char) 0x3a)
                        + ((char) UP) + codes;
            } else {
                type = "" + ((char) UP);
                codes = ((char) d.getScancode()) + type + codes;
            }
        } else {
            if ((!this.opt.caps_sends_up_and_down)
                    && (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK)) {
                logger.debug("Sending CAPSLOCK toggle");
                codes += "" + ((char) 0x3a) + ((char) DOWN) + ((char) 0x3a)
                        + ((char) UP);
            } else {
                type = "" + ((char) DOWN);
                codes += ((char) d.getScancode()) + type;
            }
        }

        return codes;
    }
}
