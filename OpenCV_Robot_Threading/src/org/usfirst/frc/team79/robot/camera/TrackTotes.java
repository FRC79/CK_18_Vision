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
    		if(vision.cameraConnected() && vision.processingImage()){
    			trackingStarted = true;
    		}
    	} else {
    		System.out.println("X: " + vision.getToteX() + ", Y: " + vision.getToteY());
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
