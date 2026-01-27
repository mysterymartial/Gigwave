import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GigCard from '../../components/GigCard';
import { Gig, GigStatus } from '../../types';

const mockGig: Gig = {
  id: '123',
  organizerId: '456',
  title: 'Test Gig',
  description: 'Test Description',
  eventDate: new Date().toISOString(),
  location: 'Lagos',
  budgetMin: 50000,
  budgetMax: 100000,
  status: GigStatus.OPEN,
  createdAt: new Date().toISOString(),
};

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('GigCard', () => {
  test('should render gig information', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
    expect(screen.getByText('Test Description')).toBeInTheDocument();
    expect(screen.getByText('Lagos')).toBeInTheDocument();
  });

  test('should display budget range', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText(/₦50,000/)).toBeInTheDocument();
    expect(screen.getByText(/₦100,000/)).toBeInTheDocument();
  });

  test('should handle zero budget', () => {
    // Boundary: zero budget
    const zeroBudgetGig = { ...mockGig, budgetMin: 0, budgetMax: 0 };
    renderWithRouter(<GigCard gig={zeroBudgetGig} />);
    
    expect(screen.getByText(/₦0/)).toBeInTheDocument();
  });

  test('should handle very large budget', () => {
    // Boundary: very large budget
    const largeBudgetGig = { ...mockGig, budgetMin: 1000000000, budgetMax: 2000000000 };
    renderWithRouter(<GigCard gig={largeBudgetGig} />);
    
    expect(screen.getByText(/₦1,000,000,000/)).toBeInTheDocument();
  });

  test('should display correct status badge', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText('OPEN')).toBeInTheDocument();
  });

  test('should handle different statuses', () => {
    // Boundary: different status values
    const cancelledGig = { ...mockGig, status: GigStatus.CANCELLED };
    renderWithRouter(<GigCard gig={cancelledGig} />);
    
    expect(screen.getByText('CANCELLED')).toBeInTheDocument();
  });

  test('should handle empty description', () => {
    // Boundary: empty description
    const emptyDescGig = { ...mockGig, description: '' };
    renderWithRouter(<GigCard gig={emptyDescGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });

  test('should handle null location', () => {
    // Boundary: null location
    const nullLocationGig = { ...mockGig, location: '' };
    renderWithRouter(<GigCard gig={nullLocationGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });
});


import GigCard from '../../components/GigCard';
import { Gig, GigStatus } from '../../types';

const mockGig: Gig = {
  id: '123',
  organizerId: '456',
  title: 'Test Gig',
  description: 'Test Description',
  eventDate: new Date().toISOString(),
  location: 'Lagos',
  budgetMin: 50000,
  budgetMax: 100000,
  status: GigStatus.OPEN,
  createdAt: new Date().toISOString(),
};

const renderWithRouter = (component: React.ReactElement) => {
  return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('GigCard', () => {
  test('should render gig information', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
    expect(screen.getByText('Test Description')).toBeInTheDocument();
    expect(screen.getByText('Lagos')).toBeInTheDocument();
  });

  test('should display budget range', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText(/₦50,000/)).toBeInTheDocument();
    expect(screen.getByText(/₦100,000/)).toBeInTheDocument();
  });

  test('should handle zero budget', () => {
    // Boundary: zero budget
    const zeroBudgetGig = { ...mockGig, budgetMin: 0, budgetMax: 0 };
    renderWithRouter(<GigCard gig={zeroBudgetGig} />);
    
    expect(screen.getByText(/₦0/)).toBeInTheDocument();
  });

  test('should handle very large budget', () => {
    // Boundary: very large budget
    const largeBudgetGig = { ...mockGig, budgetMin: 1000000000, budgetMax: 2000000000 };
    renderWithRouter(<GigCard gig={largeBudgetGig} />);
    
    expect(screen.getByText(/₦1,000,000,000/)).toBeInTheDocument();
  });

  test('should display correct status badge', () => {
    renderWithRouter(<GigCard gig={mockGig} />);
    
    expect(screen.getByText('OPEN')).toBeInTheDocument();
  });

  test('should handle different statuses', () => {
    // Boundary: different status values
    const cancelledGig = { ...mockGig, status: GigStatus.CANCELLED };
    renderWithRouter(<GigCard gig={cancelledGig} />);
    
    expect(screen.getByText('CANCELLED')).toBeInTheDocument();
  });

  test('should handle empty description', () => {
    // Boundary: empty description
    const emptyDescGig = { ...mockGig, description: '' };
    renderWithRouter(<GigCard gig={emptyDescGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });

  test('should handle null location', () => {
    // Boundary: null location
    const nullLocationGig = { ...mockGig, location: '' };
    renderWithRouter(<GigCard gig={nullLocationGig} />);
    
    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });
});

