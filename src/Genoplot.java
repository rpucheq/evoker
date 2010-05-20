import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.LinkedList;

public class Genoplot extends JFrame implements ActionListener {

    private DataDirectory db;

    String plottedSNP = null;
    
    private JTextField snpField;
    private JButton goButton;
    private JPanel plotArea;

    private PrintStream output;
    private LinkedList<String> snpList;
    private String currentSNPinList;
    private String currentSNP = null;
    private int currentSNPindex;
    private int displaySNPindex;
    private boolean backSNP    = false;
    private boolean historySNP = false;
    private boolean markerList = false;
    private Hashtable<String,Integer> listScores = new Hashtable<String,Integer>();

    JFileChooser jfc;
    DataConnectionDialog dcd;
    PDFDialog pdfd;
    
    private JPanel scorePanel;
    private JPanel messagePanel;
    private JLabel message;
    private JButton yesButton;
    private JButton maybeButton;
    private JButton noButton;
    private JButton backButton;
    private JMenu fileMenu;
    private JMenuItem loadList;
    private JMenuItem loadExclude;
    private JMenu toolsMenu;
    private JCheckBoxMenuItem filterData;
    private JMenuItem saveAll;
    private JMenuItem exportPDF;
    private JMenu viewMenu;
    private JMenuItem viewPolar;
    private JMenuItem viewCart;
    private JMenu historyMenu;
    private ButtonGroup snpGroup;
    private JMenuItem returnToListPosition;
   
    public static LoggingDialog ld;
    private JButton randomSNPButton;

	private PDFFile allPDF = null;
	private PDFFile yesPDF = null;
	private PDFFile maybePDF = null;
	private PDFFile noPDF = null;
	private int yesPlotNum;
	private int maybePlotNum;
	private int noPlotNum;
	
	// default
	private String coordSystem = "CART";
	
	
    public static void main(String[] args){

        new Genoplot();

    }

    Genoplot(){
        super("Evoke...");

        jfc  = new JFileChooser("user.dir");
        dcd  = new DataConnectionDialog(this);
        pdfd = new PDFDialog(this);

        JMenuBar mb = new JMenuBar();

        int menumask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        fileMenu = new JMenu("File");
        JMenuItem openDirectory = new JMenuItem("Open directory");
        openDirectory.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menumask));
        openDirectory.addActionListener(this);
        fileMenu.add(openDirectory);
        JMenuItem openRemote = new JMenuItem("Connect to remote server");
        openRemote.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menumask));
        openRemote.addActionListener(this);
        fileMenu.add(openRemote);
        loadList = new JMenuItem("Load marker list");
        loadList.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, menumask));
        loadList.addActionListener(this);
        loadList.setEnabled(false);
        fileMenu.add(loadList);
        loadExclude = new JMenuItem("Load sample exclude list");
        loadExclude.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, menumask));
        loadExclude.addActionListener(this);
        loadExclude.setEnabled(false);
        fileMenu.add(loadExclude);
        
        mb.add(fileMenu);
        
        toolsMenu = new JMenu("Tools");        
        filterData = new JCheckBoxMenuItem("Filter samples");
        filterData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, menumask));
        filterData.addActionListener(this);
        filterData.setEnabled(false);
        toolsMenu.add(filterData);
        saveAll = new JMenuItem("Save SNP Plots");
        saveAll.addActionListener(this);
        saveAll.setEnabled(false);
        toolsMenu.add(saveAll);
//        exportPDF = new JMenuItem("Generate PDF from scores");
//        exportPDF.addActionListener(this);
//        exportPDF.setEnabled(false);
//        toolsMenu.add(exportPDF);
        
        if (!(System.getProperty("os.name").toLowerCase().contains("mac"))){
            JMenuItem quitItem = new JMenuItem("Quit");
            quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menumask));
            quitItem.addActionListener(this);
            fileMenu.add(quitItem);
        }

        mb.add(toolsMenu);
        
        viewMenu = new JMenu("View");
        ButtonGroup viewGroup = new ButtonGroup();
        viewCart = new JCheckBoxMenuItem("Cartesian coordinates");
        viewCart.addActionListener(this);
        viewCart.setEnabled(false);
        viewGroup.add(viewCart);
        viewMenu.add(viewCart);
        viewPolar = new JCheckBoxMenuItem("Polar coordinates");
        viewPolar.addActionListener(this);
        viewPolar.setEnabled(false);
        viewGroup.add(viewPolar);
        viewMenu.add(viewPolar);
        
        mb.add(viewMenu);
        
        historyMenu = new JMenu("History");
        returnToListPosition = new JMenuItem("Return to current list position");
        snpGroup = new ButtonGroup();
        returnToListPosition.setEnabled(false);
        returnToListPosition.addActionListener(this);
        historyMenu.add(returnToListPosition);
        historyMenu.addSeparator();
        mb.add(historyMenu);

        JMenu logMenu = new JMenu("Log");
        JMenuItem showLogItem = new JMenuItem("Show Evoker log");
        showLogItem.addActionListener(this);
        logMenu.add(showLogItem);
        mb.add(logMenu);
               
        setJMenuBar(mb);

        JPanel controlsPanel = new JPanel();

        snpField = new JTextField(10);
        snpField.setEnabled(false);
        JPanel snpPanel = new JPanel();
        snpPanel.add(new JLabel("SNP:"));
        snpPanel.add(snpField);
        goButton = new JButton("Go");
        goButton.addActionListener(this);
        goButton.setEnabled(false);
        snpPanel.add(goButton);
        randomSNPButton = new JButton("Random");
        randomSNPButton.addActionListener(this);
        randomSNPButton.setEnabled(false);
        snpPanel.add(randomSNPButton);
        controlsPanel.add(snpPanel);

        controlsPanel.add(Box.createRigidArea(new Dimension(50,1)));

        scorePanel = new JPanel();
        scorePanel.add(new JLabel("Approve?"));

        yesButton = new JButton("Yes");
        scorePanel.registerKeyboardAction(this,"Yes",
                KeyStroke.getKeyStroke(KeyEvent.VK_Y,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        yesButton.addActionListener(this);
        yesButton.setEnabled(false);
        scorePanel.add(yesButton);

        maybeButton = new JButton("Maybe");
        scorePanel.registerKeyboardAction(this,"Maybe",
                KeyStroke.getKeyStroke(KeyEvent.VK_M,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        maybeButton.addActionListener(this);
        maybeButton.setEnabled(false);
        scorePanel.add(maybeButton);

        noButton = new JButton("No");
        scorePanel.registerKeyboardAction(this,"No",
                KeyStroke.getKeyStroke(KeyEvent.VK_N,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        noButton.addActionListener(this);
        noButton.setEnabled(false);
        scorePanel.add(noButton);

        backButton = new JButton("Back");
        scorePanel.registerKeyboardAction(this,"Back",
                KeyStroke.getKeyStroke(KeyEvent.VK_B,0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        backButton.addActionListener(this);
        backButton.setEnabled(false);
        scorePanel.add(backButton);
        
        messagePanel = new JPanel();
        message      = new JLabel("");
        message.setEnabled(false);
        messagePanel.add(message);
        message.setVisible(false);
                
        JPanel rightPanel = new JPanel();
        rightPanel.add(scorePanel);
        rightPanel.add(messagePanel);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        controlsPanel.add(rightPanel);
        
        controlsPanel.setMaximumSize(new Dimension(2000,(int)controlsPanel.getPreferredSize().getHeight()));
        controlsPanel.setMinimumSize(new Dimension(10,(int)controlsPanel.getPreferredSize().getHeight()));

        plotArea = new JPanel();
        plotArea.setPreferredSize(new Dimension(700,350));
        plotArea.setBorder(new LineBorder(Color.BLACK));
        plotArea.setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(plotArea);
        contentPanel.add(controlsPanel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        });

        this.setContentPane(contentPanel);
        this.pack();
        this.setVisible(true);

        ld = new LoggingDialog(this);
        ld.pack();
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try{
            String command = actionEvent.getActionCommand();
            if (command.equals("Go")){
                plotIntensitas(snpField.getText().trim());
            }else if (command.equals("No")){
                noButton.requestFocusInWindow();
                recordVerdict(-1);
            }else if (command.equals("Maybe")){
                maybeButton.requestFocusInWindow();
                recordVerdict(0);
            }else if (command.equals("Yes")){
                yesButton.requestFocusInWindow();
                recordVerdict(1);
            }else if (command.equals("Back")){
            	setBackSNP(true);
            	displaySNPindex--;
            	if (displaySNPindex == 0) {
            		backButton.setEnabled(false);
            	}
            	int lastCall = listScores.get(snpList.get(displaySNPindex));
            	if (lastCall == 1){
            		yesButton.requestFocusInWindow();
            	} else if (lastCall == 0) {
            		maybeButton.requestFocusInWindow();
            	} else {
            		noButton.requestFocusInWindow();
            	}            	
            	plotIntensitas(snpList.get(displaySNPindex));
            }else if (command.equals("Return to current list position")){
                plotIntensitas(currentSNPinList);
            }else if (command.equals("Random")){
                plotIntensitas(db.getRandomSNP());
            }else if (command.startsWith("PLOTSNP")){
                String[] bits = command.split("\\s");
                plotIntensitas(bits[1]);
            }else if (command.equals("Open directory")){
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                    try{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        db = new DataDirectory(jfc.getSelectedFile().getAbsolutePath());
                        finishLoadingDataSource();
                    }finally{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }else if (command.equals("Connect to remote server")){
                dcd.pack();
                dcd.setVisible(true);
                DataClient dc = new DataClient(dcd);
                if (dc.getConnectionStatus()){
                    try{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        db = new DataDirectory(dc);
                        finishLoadingDataSource();
                    }finally{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }else if (command.equals("Load marker list")){
            	currentSNPindex = 0;
                displaySNPindex = currentSNPindex;
            	jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                    loadList(jfc.getSelectedFile().getAbsolutePath());
                }
                File outfile = checkOverwriteFile(new File(jfc.getSelectedFile().getAbsolutePath()+".scores"));
                FileOutputStream fos = new FileOutputStream(outfile);
                output = new PrintStream(fos);
                
            }else if (command.equals("Load sample exclude list")) {
            	jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            	if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            		// TODO: before setting as the new list check it is a valid not empty exclude file
            		db.setExcludeList(new QCFilterData(jfc.getSelectedFile().getAbsolutePath()));
            		db.setFilterState(true);
            		filterData.setEnabled(true);
            		filterData.setSelected(true);
            		Genoplot.ld.log("Loaded exclude file: " + jfc.getSelectedFile().getName());
            		if (currentSNP != null){
                		plotIntensitas(currentSNP);
                	}
                }
            }else if (command.equals("Filter samples")){
            	// turn filtering on/off
            	if (filterData.isSelected()) {
            		db.setFilterState(true);
            	} else {
            		db.setFilterState(false);
            	}
            	if (currentSNP != null){
            		plotIntensitas(currentSNP);
            	}
            }else if (command.equals("Save SNP Plots")){
            	File defaultFileName = new File(plottedSNP + ".png");
            	jfc.setSelectedFile(defaultFileName);
            	if(jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
            		File png;
            		if (!jfc.getSelectedFile().toString().endsWith(".png")){
            			png = new File(jfc.getSelectedFile().toString() + ".png");
            		} else{
            			png = jfc.getSelectedFile();
            		}
            		BufferedImage image = new BufferedImage(plotArea.getWidth(), plotArea.getHeight(), BufferedImage.TYPE_INT_RGB);
            		Graphics2D g2 = image.createGraphics();
                	plotArea.paint(g2);
                    g2.dispose();
                    try {
                      	ImageIO.write(image, "png", png);
                    }
                    catch(IOException ioe) {
                    	System.out.println(ioe.getMessage());
                    }
            	}                
            }else if (command.equals("Generate PDF from scores")){
            	pdfd.pack();
            	pdfd.setVisible(true);
            	if(pdfd.success()){
            		try{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        generatePDFs();
                    }finally{
                        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
            	}
            }
            else if (command.equals("Cartesian coordinates")) {
            	setCoordSystem("CART");
            	if (currentSNP != null){
            		plotIntensitas(currentSNP);
            	}
            }else if (command.equals("Polar coordinates")) {
            	setCoordSystem("POLAR");
            	if (currentSNP != null){
            		plotIntensitas(currentSNP);
            	}            	
            }else if (command.equals("Show Evoker log")){
                ld.setVisible(true);
            }else if (command.equals("Quit")){
                System.exit(0);
            }
        }catch (IOException ioe){
            JOptionPane.showMessageDialog(this,ioe.getMessage(),"File error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	private void generatePDFs() throws DocumentException, IOException {
		try{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            openPDFs();
        	//TODO: progress bar?    	
        	BufferedReader listReader = new BufferedReader(new FileReader(pdfd.getscoresFile()));
            String currentLine;
            while ((currentLine = listReader.readLine()) != null){
                String[] bits = currentLine.split("\t");
                String snp = bits[0];
                String score  = bits[1];
                
                //TODO make sure the snp is in the current data set      
                System.out.println(snp + "  " + score);
                // create plot panel
                plotIntensitas(snp);
                JFrame test = new JFrame();
                test.add(plotArea);
                test.setVisible(true);
                
                // print plot panel to pdf
                if (pdfd.allPlots()) {
        			allPDF.writePanel2PDF(plotArea);
        		}
        		if (score.contains("1") && pdfd.yesPlots()) {
        			yesPDF.writePanel2PDF(plotArea);
        			yesPlotNum++;
        		}
        		if (score.contains("0") && pdfd.maybePlots()) {
        			maybePDF.writePanel2PDF(plotArea);
        			maybePlotNum++;
        		}
        		if (score.contains("-1") && pdfd.noPlots()) {
        			noPDF.writePanel2PDF(plotArea);
        			noPlotNum++;
        		}
            }
            System.out.println(yesPlotNum + "  " + maybePlotNum + "  " + noPlotNum);
            listReader.close();
        	closePDFs();
        }finally{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
	}
	
	private void printScores() {
    	Enumeration<String> snps = listScores.keys();
        while (snps.hasMoreElements()){
          String snp = (String) snps.nextElement();
          output.println(snp + "\t" + listScores.get(snp));
        }
        output.close();
        // now you can convert these scores into a pdf
        //exportPDF.setEnabled(true);
    }
	
	private void setMarkerList(boolean marker) {
    	markerList = marker;
    }
    
    private boolean markerListLoaded() {
		return markerList;
	}
	
	private void activeScorePanel(boolean score) {
		if (score) {
			yesButton.setEnabled(true);
            noButton.setEnabled(true);
            maybeButton.setEnabled(true);
            backButton.setEnabled(true);
            scorePanel.setEnabled(true);
		} else {
			yesButton.setEnabled(false);
            noButton.setEnabled(false);
            maybeButton.setEnabled(false);
            backButton.setEnabled(false);
            scorePanel.setEnabled(false);
		}
	}
	
	private void setBackSNP(boolean back) {
		backSNP = back;
	}
    
    private boolean isBackSNP() {
    	return backSNP;
    }

	private void setHistorySNP(boolean history) {
		historySNP = history;
	}

	private boolean isHistorySNP() {
		return historySNP;
	}
	
	private void setCoordSystem(String s) {
		coordSystem = s;
	}
    
    private String getCoordSystem() {
    	return coordSystem;
    }

	private File checkOverwriteFile(File file) {
 
    	if (file.exists()) {
        	int n = JOptionPane.showConfirmDialog(
        			null, 
        			"The file " + file.getName() + " already exists\n would you like to overwrite this file?",
        			"Overwrite file?",
        			JOptionPane.YES_NO_OPTION,
        			JOptionPane.QUESTION_MESSAGE );
        	// n 0 = yes 1 = no
        	if (n == 1) {		
        		if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
        			file = new File(jfc.getSelectedFile().getAbsolutePath());
                }
        	}
        }
    	return file;
	}
   
    private void recordVerdict(int v) throws DocumentException{
    	if (isBackSNP()) {
       		listScores.put(snpList.get(displaySNPindex), v);
       		setBackSNP(false);
       		if (displaySNPindex == 0) {
        		backButton.setEnabled(true);
        	}
       		displaySNPindex = currentSNPindex;
       		plotIntensitas(snpList.get(currentSNPindex));
       	} else if (isHistorySNP()) {
       		listScores.put(snpList.get(displaySNPindex), v);
       		setHistorySNP(false);
       		if (displaySNPindex == 0) {
        		backButton.setEnabled(true);
        	}
       		displaySNPindex = currentSNPindex;
       		plotIntensitas(snpList.get(currentSNPindex));
       	} else {	
       		listScores.put(snpList.get(currentSNPindex), v);
       		if (currentSNPindex < (snpList.size()-1)){
       			currentSNPindex++;
       			displaySNPindex = currentSNPindex;
       			backButton.setEnabled(true);
       			plotIntensitas(snpList.get(currentSNPindex));
           	}else{
           		plotIntensitas("ENDLIST");
           		activeScorePanel(false);
           		returnToListPosition.setEnabled(false);
           		int n = JOptionPane.showConfirmDialog(
            			null, 
            			"Would you like to save the scores you have generated?",
            			"Finish scoring list?",
            			JOptionPane.YES_NO_OPTION,
            			JOptionPane.QUESTION_MESSAGE );
            	// n 0 = yes 1 = no
            	if (n == 0) {
            		printScores();
            		setMarkerList(false);
            	} else {
            		activeScorePanel(true);
            		plotIntensitas(snpList.get(currentSNPindex));
            	}
           	}
       	}
    }
    
    private void printMessage(String string) {
		message.setText(string);
    	message.setVisible(true);
	}

	private void openPDFs() throws DocumentException, IOException {
		
    	if (pdfd.allPlots()) {
			allPDF = new PDFFile(checkOverwriteFile(new File(pdfd.getPdfDir() + "/all.pdf")));
    	}
    	if (pdfd.yesPlots()) {
    		yesPDF = new PDFFile(checkOverwriteFile(new File(pdfd.getPdfDir() + "/yes.pdf")));
    		yesPlotNum = 0;
    		
    	}
    	if (pdfd.maybePlots()) {
    		maybePDF = new PDFFile(checkOverwriteFile(new File(pdfd.getPdfDir() + "/maybe.pdf")));
    		maybePlotNum = 0;
    	
    	}
    	if (pdfd.noPlots()) {
    		noPDF = new PDFFile(checkOverwriteFile(new File(pdfd.getPdfDir() + "/no.pdf")));
    		noPlotNum = 0;
    	}
	}
    
    private void closePDFs() throws DocumentException {
    	if (pdfd.allPlots() && allPDF.isFileOpen()) {
    		allPDF.getDocument().close();
        }
        if (pdfd.yesPlots() && yesPDF.isFileOpen()) {
        	
        	if (yesPlotNum == 0) {
        		yesPDF.getDocument().add(new Paragraph("No Yes plots recorded"));
        	}
        	yesPDF.getDocument().close();
        }
    	if (pdfd.maybePlots() && maybePDF.isFileOpen()) {
    		
    		if (maybePlotNum == 0) {
        		maybePDF.getDocument().add(new Paragraph("No Maybe plots recorded"));
        	}
    		maybePDF.getDocument().close();
        }
    	if (pdfd.noPlots() && noPDF.isFileOpen()) {
    		
    		if (noPlotNum == 0) {
        		noPDF.getDocument().add(new Paragraph("No No plots recorded"));
        	}
    		noPDF.getDocument().close();
        }	
	}
    
    private void viewedSNP(String name){

        boolean inHistory = false;
        for (Component i : historyMenu.getMenuComponents()){
            if (i instanceof  JMenuItem){
                if (((JMenuItem)i).getText().equals(name)){
                    ((JRadioButtonMenuItem)i).setSelected(true);
                    inHistory = true;
                    break;
                }
            }
        }
        
        if (!inHistory){
            //new guy
            JRadioButtonMenuItem snpItem = new JRadioButtonMenuItem(name);
            snpItem.setActionCommand("PLOTSNP "+name);
            snpItem.addActionListener(this);
            snpGroup.add(snpItem);
            snpItem.setSelected(true);
            historyMenu.add(snpItem,2);

            //only track last ten
            if (historyMenu.getMenuComponentCount() > 12){
                historyMenu.remove(12);
            }
        }
    }
    
//	private void viewedSNP(String name){
//
//        boolean alreadyHere = false;
//        for (Component i : historyMenu.getMenuComponents()){
//            if (i instanceof  JMenuItem){
//                if (((JMenuItem)i).getText().equals(name)){
//                    ((JRadioButtonMenuItem)i).setSelected(true);
//                    alreadyHere = true;
//                    break;
//                }
//            }
//        }
//
//        if (alreadyHere){
//            if (currentSNPinList != null){
//                if (snpGroup.getSelection().getActionCommand().equals("PLOTSNP "+currentSNPinList)){
//                    yesButton.setEnabled(true);
//                    noButton.setEnabled(true);
//                    maybeButton.setEnabled(true);                    
//                    scorePanel.setEnabled(true);
//                    message.setVisible(false);
//                }else if (isBackSNP){
//                	yesButton.setEnabled(true);
//                    noButton.setEnabled(true);
//                    maybeButton.setEnabled(true);                    
//                    scorePanel.setEnabled(true);
//                    message.setVisible(false);
//                }else{
//                    //we're viewing a SNP from the history, so we can't allow
//                    //the user to take any action on a SNP list (if one exists) because
//                    //we're not viewing the "active" SNP from the list
//                	// disable the score panel to also stop key stroke events
//                	yesButton.setEnabled(false);
//                    noButton.setEnabled(false);
//                    maybeButton.setEnabled(false);
//                    scorePanel.setEnabled(false);
//                    printMessage("SNP already scored");
//                    //TODO: actually allow you to go back and change your mind?
//                }
//            }
//        }else{
//            //new guy
//            JRadioButtonMenuItem snpItem = new JRadioButtonMenuItem(name);
//            snpItem.setActionCommand("PLOTSNP "+name);
//            snpItem.addActionListener(this);
//            snpGroup.add(snpItem);
//            snpItem.setSelected(true);
//            historyMenu.add(snpItem,2);
//
//            //only track last ten
//            if (historyMenu.getMenuComponentCount() > 12){
//                historyMenu.remove(12);
//            }
//        }
//
//    }

	private void finishLoadingDataSource(){
        if (db != null){
            if (db.getCollections().size() == 3){
                this.setSize(new Dimension(1000,420));
            }else if (db.getCollections().size() == 4){
                this.setSize(new Dimension(700,750));
            }
            goButton.setEnabled(true);
            randomSNPButton.setEnabled(true);
            snpField.setEnabled(true);
            loadList.setEnabled(true);
            loadExclude.setEnabled(true);
            saveAll.setEnabled(true);
            viewCart.setEnabled(true);
            viewPolar.setEnabled(true);
            while(historyMenu.getMenuComponentCount() > 2){
                historyMenu.remove(2);
            }
            if(db.qcList() != null){
            	// if a exclude file is loaded from the directory enable filtering
            	filterData.setEnabled(true);
            	filterData.setSelected(true);
            }

            this.setTitle("Evoke...["+db.getDisplayName()+"]");

            plotArea.removeAll();
            plotArea.repaint();
        }
    }

    private void plotIntensitas(String name){
    	plottedSNP = name;
    	plotArea.removeAll();
        plotArea.setLayout(new BoxLayout(plotArea,BoxLayout.Y_AXIS));
        if (name != null){
            if (!name.equals("ENDLIST")){
                currentSNP = name;
            	plotArea.add(new JLabel(name));
                fetchRecord(name);
                viewedSNP(name);
            }else{
                //I tried very hard to get the label right in the middle and failed because java layouts blow
                plotArea.add(Box.createVerticalGlue());
                JPanel p = new JPanel();
                p.add(new JLabel("End of list."));
                p.setBackground(Color.WHITE);
                plotArea.add(p);
                plotArea.add(Box.createVerticalGlue());
            }
        }

        //seems to need both of these to avoid floating old crud left behind
        plotArea.revalidate();
        plotArea.repaint();
    }

    private void loadList(String filename)throws IOException{
    	snpList = new LinkedList<String>();
        BufferedReader listReader = new BufferedReader(new FileReader(filename));
        String currentLine;
        while ((currentLine = listReader.readLine()) != null){
            String[] bits = currentLine.split("\n");
            snpList.add(bits[0]);
        }
        listReader.close();
        
        setMarkerList(true);

        try{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            db.listNotify((LinkedList)snpList.clone());
            displaySNPindex = currentSNPindex;
            plotIntensitas(snpList.get(currentSNPindex));
            returnToListPosition.setEnabled(true);
            yesButton.setEnabled(true);
            noButton.setEnabled(true);
            maybeButton.setEnabled(true);
            yesButton.requestFocusInWindow();
        }finally{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void fetchRecord(String name){

        try{
            if (db.isRemote()){
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            JPanel plotHolder = new JPanel();
            plotHolder.setBackground(Color.WHITE);
            plotArea.add(plotHolder);


            Vector<String> v = db.getCollections();
            Vector<PlotPanel> plots = new Vector<PlotPanel>();
            double maxdim=-100000;
            double mindim=100000;
            for (String c : v){
                PlotPanel pp = new PlotPanel(c,db.getRecord(name, c, getCoordSystem()));

                pp.refresh();
                if (pp.getMaxDim() > maxdim){
                    maxdim = pp.getMaxDim();
                }
                if (pp.getMinDim() < mindim){
                    mindim = pp.getMinDim();
                }
                plots.add(pp);
            }

            for (PlotPanel pp : plots){
                pp.setDimensions(mindim,maxdim);
                plotHolder.add(pp);
            }
        }catch (IOException ioe){
            JOptionPane.showMessageDialog(this,ioe.getMessage(),"File error",
                    JOptionPane.ERROR_MESSAGE);
        }finally{
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
