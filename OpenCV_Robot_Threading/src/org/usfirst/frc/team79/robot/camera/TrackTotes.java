package org.usfirst.frc.team79.robot.camera;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class TrackTotes extends Command {

	VisionService vision;
	boolean trackingStarted;
	
    public TrackTotes() {
    	vision = VisionService.getInstance();
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	trackingStarted = false;
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	if(!trackingStarted){
    		if(vision.cameraConnected()){
    			trackingStarted = true;
    		}
    	} else {
    		// Fun things
    		// Most likely will not need a "TrackTotes" Command
    		// Can probably just call VisionService which would return
    		// the x displacement from the tote.
    		// System.out.println("X DISPL: " + vision.getXDisplacement());
    	}
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
