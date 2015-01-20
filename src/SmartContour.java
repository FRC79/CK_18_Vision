import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;


public class SmartContour {
	double left, right, top, bottom;
	double width, height, area, aspectRatio;
	Point center;
	
	SmartContour(MatOfPoint contour){
		Point[] points = contour.toArray();
		double left = Double.MAX_VALUE, right = 0, bottom = Double.MAX_VALUE, top = 0;
		for(Point p : points){
			left = (p.x < left) ? p.x : left;
			right = (p.x > right) ? p.x : right;
			bottom = (p.y < bottom) ? p.y : bottom;
			top = (p.y > top) ? p.y : top;
		}
		
		// Scoring
		this.width = right - left;
		this.height = top - bottom;
		this.area = width * height;
		this.aspectRatio = width / height;
		this.center = new Point(right - (width / 2.0), top - (height / 2.0));
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
