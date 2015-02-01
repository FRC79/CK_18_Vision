import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;


public class SmartContour {
	double left, right, top, bottom;
	double width, height, area, aspectRatio;
	Point center, topLeft, bottomRight, avgPoint;
	boolean isLeft = true, isRight = true;
	
	SmartContour(MatOfPoint contour){
		Point[] points = contour.toArray();
		double left = Double.MAX_VALUE, right = 0, bottom = 0, top = Double.MAX_VALUE;
		for(Point p : points){
			left = (p.x < left) ? p.x : left;
			right = (p.x > right) ? p.x : right;
			bottom = (p.y > bottom) ? p.y : bottom;
			top = (p.y < top) ? p.y : top;
		}

		// Scoring
		this.width = right - left;
		this.height = bottom - top;
		this.area = width * height;
		this.aspectRatio = width / height;
		this.center = new Point(right - (width / 2.0), bottom - (height / 2.0));
		this.topLeft = new Point(left, top);
		this.bottomRight = new Point(right, bottom);
		
		// X values get larger near the right of the image
		// Y values get larger near the bottom of the image
		for(Point p : points){
			if(p.y < center.y){
				if(p.x < center.x){
					isLeft = false;
				}
				if(p.x > center.x){
					isRight = false;
				}
			}
		}
	}
	
	public boolean isLeft(){
		return isLeft;
	}
	
	public boolean isRight(){
		return isRight;
	}
	
	public Point getTopLeft(){
		return topLeft;
	}
	
	public Point getBottomRight(){
		return bottomRight;
	}

	public double getLeft() {
		return left;
	}

	public double getRight() {
		return right;
	}

	public double getTop() {
		return top;
	}

	public double getBottom() {
		return bottom;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	public double getArea() {
		return area;
	}

	public double getAspectRatio() {
		return aspectRatio;
	}

	public Point getCenter() {
		return center;
	}
	
}
