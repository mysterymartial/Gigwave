import React from 'react';
import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import GigCard from '../../components/GigCard';
import { Gig, GigStatus } from '../../types';
import { profileApi } from '../../lib/api';

vi.mock('../../lib/api');

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
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    vi.mocked(profileApi.getOrganizerProfileByUserId).mockRejectedValue(new Error('no profile'));
  });

  test('should render gig information', () => {
    renderWithRouter(<GigCard gig={mockGig} />);

    expect(screen.getByText('Test Gig')).toBeInTheDocument();
    expect(screen.getByText('Lagos')).toBeInTheDocument();
  });

  test('should display budget', () => {
    renderWithRouter(<GigCard gig={mockGig} />);

    expect(screen.getByText(/₦100,000/)).toBeInTheDocument();
  });

  test('should handle zero budget', () => {
    const zeroBudgetGig = { ...mockGig, budgetMin: 0, budgetMax: 0 };
    renderWithRouter(<GigCard gig={zeroBudgetGig} />);

    expect(screen.getByText(/₦0/)).toBeInTheDocument();
  });

  test('should handle very large budget', () => {
    const largeBudgetGig = { ...mockGig, budgetMin: 1000000000, budgetMax: 2000000000 };
    renderWithRouter(<GigCard gig={largeBudgetGig} />);

    expect(screen.getByText(/₦2,000,000,000/)).toBeInTheDocument();
  });

  test('should display gig tag badge', () => {
    renderWithRouter(<GigCard gig={mockGig} />);

    expect(screen.getByText('Gig')).toBeInTheDocument();
  });

  test('should show View Details link', () => {
    renderWithRouter(<GigCard gig={mockGig} />);

    expect(screen.getByRole('link', { name: /view details/i })).toBeInTheDocument();
  });

  test('should handle empty description', () => {
    const emptyDescGig = { ...mockGig, description: '' };
    renderWithRouter(<GigCard gig={emptyDescGig} />);

    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });

  test('should handle empty location', () => {
    const nullLocationGig = { ...mockGig, location: '' };
    renderWithRouter(<GigCard gig={nullLocationGig} />);

    expect(screen.getByText('Test Gig')).toBeInTheDocument();
  });
});
