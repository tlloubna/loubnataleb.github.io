/*
 * The Forceps model.
 * 
 * Copyright (C) April 2013: X. Morin (CNRS CEFE).
 * 
 * This file is part of the Forceps model and is NOT free software. It is the property of its
 * authors and must not be copied without their permission. It can be shared by the modellers of the
 * Capsis co-development community in agreement with the Capsis charter
 * (http://capsis.cirad.fr/capsis/charter). See the license.txt file in the Capsis installation
 * directory for further information about licenses in Capsis.
 */

package forceps.extension.intervener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import jeeb.lib.util.AmapDialog;
import jeeb.lib.util.LinePanel;
import jeeb.lib.util.Translator;
import capsis.commongui.util.Helper;


/**
 * This dialog box is used to set ...
* @author Ph. Dreyfus - December 2017
 *   it is almost a copy of GymnoGHAThinner2Dialog
 */
public class CepsGHAThinnerDialog extends AmapDialog implements ActionListener {

	private NumberFormat nf;
	private CepsGHAThinner thinner;
	private JSlider intensitySlider;
	private JSlider typeSlider;
	protected JButton ok;
	protected JButton cancel;
	protected JButton help;

	/**
	 * Constructor
	 */
	public CepsGHAThinnerDialog (CepsGHAThinner thinner) {
		super ();

		this.thinner = thinner;

		// To show numbers in a nice way
		nf = NumberFormat.getInstance (Locale.ENGLISH);
		nf.setGroupingUsed (false);
		nf.setMaximumFractionDigits (3);

		createUI ();
		
		// fc-31.8.2018
		setTitle (thinner.getName());
//		setTitle (Translator.swap ("CepsGHAThinnerDialog"));

		setModal (true);

		// location is set by the AmapDialog superclass
		pack (); // uses component's preferredSize
		show ();

	}

	/**
	 * Action on ok button.
	 */
	private void okAction () {
		// All has been checked successfully, set the dialog invisible
		// and go back to caller (will check for validity and dispose the dialog)
		setValidDialog (true);
	}

	/**
	 * Action on cancel button.
	 */
	private void cancelAction () {
		// Set the dialog invisible
		// and go back to caller (will check for validity and dispose the dialog)
		setValidDialog (false);
	}

	/**
	 * Someone hit a button.
	 */
	public void actionPerformed (ActionEvent evt) {
		if (evt.getSource () instanceof JRadioButton) {
		} else if (evt.getSource ().equals (ok)) {
			okAction ();
		} else if (evt.getSource ().equals (cancel)) {
			cancelAction ();
		} else if (evt.getSource ().equals (help)) {
			Helper.helpFor (this);
		}
	}

	/**
	 * Creates the dialog box user interface.
	 */
	private void createUI () {

		// vertical panel for sliders
		Box sliderPanel = Box.createVerticalBox ();

		double initGHA = thinner.getInitGHA();

		//sliders
		int min = 0;
		int max = (int) initGHA + 1;
		int initialValue = (int) initGHA + 1;

		//density slider
		LinePanel l1 = new LinePanel (Translator.swap ("CepsGHAThinnerDialog.targetGHA"));
		intensitySlider = new JSlider (JSlider.HORIZONTAL, min, max, initialValue);
		intensitySlider.setMajorTickSpacing(5);
		intensitySlider.setMinorTickSpacing(1);
		intensitySlider.setPaintTicks (true);
		intensitySlider.setPaintLabels (true);
		intensitySlider.setSnapToTicks(true);
		l1.add(intensitySlider);
		l1.addStrut0 ();

		min = 0;
		max = 100;
		initialValue = 50;

		//type slider
		LinePanel l2 = new LinePanel (Translator.swap ("CepsGHAThinnerDialog.type"));
		typeSlider = new JSlider (JSlider.HORIZONTAL, min, max, initialValue);
		typeSlider.setMajorTickSpacing(100);
		typeSlider.setMinorTickSpacing(10);
		typeSlider.setPaintTicks (true);
		typeSlider.setPaintLabels (true);
		Hashtable <Integer, JComponent> typePaintlabels = new Hashtable <Integer, JComponent> ();
		typePaintlabels.put(0, new JLabel (Translator.swap ("CepsGHAThinnerDialog.fromBelow")));
		typePaintlabels.put(50, new JLabel (Translator.swap ("CepsGHAThinnerDialog.random")));
		typePaintlabels.put(100, new JLabel (Translator.swap ("CepsGHAThinnerDialog.fromAbove")));
		typeSlider.setLabelTable (typePaintlabels);
		typeSlider.setSnapToTicks(true);
		l2.add(typeSlider);
		l2.addStrut0 ();

		//put it in a nice panel
		sliderPanel.add(l1);
		sliderPanel.add(l2);

		sliderPanel.add (Box.createHorizontalStrut (400));	// minimal size of the sliders

		//Control Line : Ok Cancel Help
		LinePanel controlPanel = new LinePanel ();
		ok = new JButton (Translator.swap ("Shared.ok"));
		cancel = new JButton (Translator.swap ("Shared.cancel"));
		help = new JButton (Translator.swap ("Shared.help"));

		controlPanel.addGlue ();  // adding glue first -> the buttons will be right justified
		controlPanel.add (ok);
		controlPanel.add (cancel);
		controlPanel.add (help);
		controlPanel.addStrut0 ();

		ok.addActionListener (this); //listeners
		cancel.addActionListener (this);
		help.addActionListener (this);

		// Put the label panels to the left, the text boxes to the right
		// and control to the bottom
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (sliderPanel, BorderLayout.NORTH);
		getContentPane ().add (controlPanel, BorderLayout.SOUTH);

		// Set Ok as default (see AmapDialog)
		setDefaultButton (ok);

	}

	/**
	 * Accessor for intensity.
	 * @return target basal area
	 */
	public double getTargetBA () {
		return ((double)intensitySlider.getValue()) ;
	}

	/**
	 * Accessor for type
	 * @return in [0,1]
	 */
	public double getTypeValue () {
		return ((double)typeSlider.getValue())/100 ;
	}

}
